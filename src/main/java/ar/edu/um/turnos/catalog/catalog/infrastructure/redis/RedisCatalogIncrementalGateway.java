package ar.edu.um.turnos.catalog.catalog.infrastructure.redis;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogChanges;
import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogIncrementalGateway;
import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto.RedisCategoryValue;
import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto.RedisChangesPayload;
import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto.RedisProfessionalValue;
import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto.RedisScheduleValue;
import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.mapper.RedisPayloadMapper;
import ar.edu.um.turnos.catalog.shared.config.CatedraProperties;
import ar.edu.um.turnos.catalog.shared.error.CatedraException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter that reads the incremental feed published by the catedra in Redis
 * (contract v1, section 14).
 *
 * <p>Everything that belongs to the transport (credentials, timeouts, keys and the
 * connection itself) is owned by {@link RedisCatalogClient} and {@link RedisNamespace}; this
 * adapter only decides <em>which</em> key answers each question and turns the published JSON
 * into domain types:</p>
 *
 * <ul>
 *   <li>the version window comes from {@code current-version} and
 *       {@code oldest-available-version} (section 14.2), with {@code metadata} (14.3) as
 *       fallback when a decimal key is not published; both sources describe the same window
 *       and the fallback keeps the status endpoint alive if one of them is temporarily
 *       missing;</li>
 *   <li>{@code changes:{version}} only announces ids; the current state of each id comes
 *       from its hash (14.4), which is what makes the application of a version reflect the
 *       latest published state;</li>
 *   <li>a payload that cannot be read becomes {@code 503 CATEDRA_INVALID_RESPONSE}: what
 *       arrived is not the contract, and an unreachable cache is {@code 503
 *       CATEDRA_UNAVAILABLE} raised by the client.</li>
 * </ul>
 *
 * <p>It never logs values, passwords or tokens: only the key of the operation and the
 * reason a payload was rejected.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisCatalogIncrementalGateway implements CatalogIncrementalGateway {

    private static final String CURRENT_VERSION_KEY = "current-version";
    private static final String OLDEST_VERSION_KEY = "oldest-available-version";
    private static final String METADATA_KEY = "metadata";
    private static final String CATEGORIES_KEY = "professional-categories";
    private static final String PROFESSIONALS_KEY = "professionals";
    private static final String SCHEDULES_KEY = "weekly-schedules";
    private static final String CHANGES_PREFIX = "changes:";

    private final RedisCatalogClient client;
    private final ObjectMapper objectMapper;
    private final RedisPayloadMapper mapper;
    private final CatedraProperties properties;

    @Override
    public CatalogVersionWindow fetchVersionWindow() {
        RedisNamespace namespace = readNamespace();
        Map<String, String> metadata = client.hashEntries(namespace.key(METADATA_KEY));

        long currentVersion = readVersion(namespace.key(CURRENT_VERSION_KEY),
                metadata.get("currentVersion"), "current-version");
        long oldestVersion = readVersion(namespace.key(OLDEST_VERSION_KEY),
                metadata.get("oldestAvailableVersion"), "oldest-available-version");
        String publishedAt = firstNonBlank(metadata.get("publishedAt"));

        return CatalogVersionWindow.builder()
                .currentVersion(currentVersion)
                .oldestAvailableVersion(oldestVersion)
                .publishedAt(publishedAt)
                .build();
    }

    @Override
    public Optional<CatalogChanges> fetchChanges(long version) {
        RedisNamespace namespace = readNamespace();
        String key = namespace.key(CHANGES_PREFIX + version);
        return client.get(key).filter(value -> !value.isBlank()).map(value -> {
            RedisChangesPayload payload;
            try {
                payload = objectMapper.readValue(value, RedisChangesPayload.class);
            } catch (JsonProcessingException | IllegalArgumentException malformed) {
                throw CatedraException.invalidResponse(
                        "Redis published a changes payload for version " + version
                                + " that does not match contract v1 section 14.5.", malformed);
            }
            try {
                return mapper.toDomain(payload);
            } catch (IllegalArgumentException rejected) {
                throw CatedraException.invalidResponse(rejected.getMessage(), rejected);
            }
        });
    }

    @Override
    public Optional<ProfessionalCategory> fetchCategory(long categoryId) {
        return readRecord(CATEGORIES_KEY, categoryId, RedisCategoryValue.class, mapper::toDomain);
    }

    @Override
    public Optional<Professional> fetchProfessional(long professionalId) {
        return readRecord(PROFESSIONALS_KEY, professionalId, RedisProfessionalValue.class, mapper::toDomain);
    }

    @Override
    public Optional<WeeklySchedule> fetchWeeklySchedule(long scheduleId) {
        return readRecord(SCHEDULES_KEY, scheduleId, RedisScheduleValue.class, mapper::toDomain);
    }

    /**
     * Reads one record of a hash and maps it. A missing field means "not published
     * anymore": the caller decides what that means (it never applies a stale record).
     *
     * @param hash       hash that holds the current state of the records
     * @param id         decimal id of the record, used as field of the hash
     * @param valueType  type of the published JSON
     * @param toDomain   mapping from the published JSON to the domain record
     * @param <V>        type of the published JSON
     * @param <D>        domain type of the record
     * @return the published state, empty when the id is not published
     */
    private <V, D> Optional<D> readRecord(String hash, long id, Class<V> valueType,
                                          Function<V, D> toDomain) {
        RedisNamespace namespace = readNamespace();
        String field = Long.toString(id);
        Optional<String> published = client.hashField(namespace.key(hash), field)
                .filter(value -> !value.isBlank());
        if (published.isEmpty()) {
            return Optional.empty();
        }
        V record;
        try {
            record = objectMapper.readValue(published.get(), valueType);
        } catch (JsonProcessingException | IllegalArgumentException malformed) {
            throw CatedraException.invalidResponse(
                    "Redis published a " + hash + " record for id " + id
                            + " that does not match contract v1 section 14.4.", malformed);
        }
        try {
            return Optional.of(toDomain.apply(record));
        } catch (IllegalArgumentException rejected) {
            throw CatedraException.invalidResponse(rejected.getMessage(), rejected);
        }
    }

    /**
     * Reads one published version from the decimal key, falling back to the metadata hash
     * of section 14.3.
     *
     * @param key       full key of the decimal value
     * @param fallback  value of the metadata hash, may be null
     * @param name      name of the version, used to build the error message
     * @return the published version
     * @throws CatedraException when neither source publishes it or the value is not a number
     */
    private long readVersion(String key, String fallback, String name) {
        String published = client.get(key).filter(value -> !value.isBlank()).orElse(null);
        published = published != null ? published : firstNonBlank(fallback);
        if (published == null) {
            throw CatedraException.invalidResponse(
                    "Redis does not publish " + name + " (" + key + "); the version window is unknown.");
        }
        try {
            return Long.parseLong(published.trim());
        } catch (NumberFormatException notADecimal) {
            throw CatedraException.invalidResponse(
                    "Redis publishes " + name + " as '" + published + "', which is not a decimal version.",
                    notADecimal);
        }
    }

    private RedisNamespace readNamespace() {
        String namespace = properties.redis() == null ? null : properties.redis().readNamespace();
        return new RedisNamespace(namespace);
    }

    private static String firstNonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
