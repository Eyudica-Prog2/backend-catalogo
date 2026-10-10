package ar.edu.um.turnos.catalog.catalog.infrastructure.redis;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSyncDiagnostic;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogSyncDiagnostics;
import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto.RedisDiagnosticValue;
import ar.edu.um.turnos.catalog.shared.config.CatedraProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter that writes the breadcrumb of the private namespace of this project:
 * {@code alumnos:{groupId}:diagnostics:last-sync} (contract v1, section 14.6).
 *
 * <p>The key is rebuilt from the write namespace received from the catedra, so the private
 * namespace of this service can never be hardcoded nor leak into the published one. The
 * structure of the value, its TTL (none) and its retention are decisions of this project:
 * it is auxiliary state, never a source of truth.</p>
 *
 * <p>Any failure (unreachable cache, unreadable structure, rejected value) is propagated as
 * is and treated as a warning by the caller: a diagnostic that cannot be written never
 * changes the outcome of a synchronization.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisCatalogSyncDiagnostics implements CatalogSyncDiagnostics {

    private static final String KEY = "diagnostics:last-sync";

    private final RedisCatalogClient client;
    private final ObjectMapper objectMapper;
    private final CatedraProperties properties;

    @Override
    public void record(CatalogSyncDiagnostic diagnostic) {
        if (diagnostic == null) {
            throw new IllegalArgumentException("A synchronization diagnostic is required.");
        }
        String namespace = properties.redis() == null ? null : properties.redis().writeNamespace();
        RedisNamespace privateNamespace = new RedisNamespace(namespace);
        client.set(privateNamespace.key(KEY), toJson(diagnostic));
    }

    private String toJson(CatalogSyncDiagnostic diagnostic) {
        RedisDiagnosticValue value = new RedisDiagnosticValue(
                diagnostic.getAppliedVersion(),
                diagnostic.getCurrentVersion(),
                diagnostic.getOldestAvailableVersion(),
                diagnostic.getResult(),
                diagnostic.getDetail(),
                diagnostic.getRecordedAt());
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException | IllegalArgumentException unrepresentable) {
            throw new IllegalStateException(
                    "The synchronization diagnostic could not be serialized.", unrepresentable);
        }
    }
}
