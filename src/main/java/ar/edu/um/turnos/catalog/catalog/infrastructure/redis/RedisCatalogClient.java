package ar.edu.um.turnos.catalog.catalog.infrastructure.redis;

import ar.edu.um.turnos.catalog.shared.config.CatedraProperties;
import ar.edu.um.turnos.catalog.shared.error.CatedraException;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.CommandType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Minimal Redis client used by the catalog copy: {@code GET}, {@code HGET}, {@code HGETALL}
 * and {@code SET} and nothing else.
 *
 * <p>Decisions worth documenting:</p>
 *
 * <ul>
 *   <li><strong>Settings come from {@code CatedraProperties}</strong> (host, port, username,
 *       password and namespaces are the {@code integration} values delivered by the catedra
 *       and externalized in the environment). This keeps the connection settings in one
 *       single place and lets the connection be built only when a synchronization actually
 *       runs: an unreachable cache never delays the boot;</li>
 *   <li><strong>bounded timeouts</strong>: two seconds to connect and two seconds per
 *       command, so a cache outage fails a synchronization instead of hanging it;</li>
 *   <li><strong>connection per operation</strong>: the sync runs are infrequent and short,
 *       so owning a shared connection would only add lifecycle problems for no gain;</li>
 *   <li>every failure is translated into {@code 503 CATEDRA_UNAVAILABLE}, because Redis is
 *       part of the catedra infrastructure and a {@code 503} (never a {@code 401}) is what
 *       tells the caller that the dependency, and not its own credentials, is at fault.</li>
 * </ul>
 *
 * <p>It never logs keys' values, usernames or passwords.</p>
 */
@Component
public class RedisCatalogClient implements DisposableBean {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(2);

    private final CatedraProperties properties;
    private final Object mutex = new Object();

    private volatile RedisClient client;

    public RedisCatalogClient(CatedraProperties properties) {
        this.properties = properties;
    }

    /**
     * @param key full key
     * @return the stored value, empty when the key does not exist
     * @throws CatedraException with {@code CATEDRA_UNAVAILABLE} when Redis cannot be reached
     */
    public Optional<String> get(String key) {
        return execute("GET " + key, commands -> Optional.ofNullable(commands.get(key)));
    }

    /**
     * @param key   key of the hash
     * @param field field of the hash
     * @return the stored value, empty when the key or the field does not exist
     * @throws CatedraException with {@code CATEDRA_UNAVAILABLE} when Redis cannot be reached
     */
    public Optional<String> hashField(String key, String field) {
        return execute("HGET " + key, commands -> Optional.ofNullable(commands.hget(key, field)));
    }

    /**
     * @param key key of the hash
     * @return every field of the hash, empty when the key does not exist
     * @throws CatedraException with {@code CATEDRA_UNAVAILABLE} when Redis cannot be reached
     */
    public Map<String, String> hashEntries(String key) {
        return execute("HGETALL " + key, commands -> {
            Map<String, String> entries = commands.hgetall(key);
            return entries == null ? Map.of() : new HashMap<>(entries);
        });
    }

    /**
     * Writes a value in the private namespace of this project.
     *
     * @param key   full key
     * @param value value to store
     * @throws CatedraException with {@code CATEDRA_UNAVAILABLE} when Redis cannot be reached
     */
    public void set(String key, String value) {
        execute("SET " + key, commands -> {
            commands.set(key, value);
            return Optional.empty();
        });
    }

    /**
     * Runs one command on a private connection and translates transport failures into the
     * functional error of the integration.
     *
     * @param operation description of the command, used only to build the error message
     * @param work      command to run
     * @param <T>       type returned by the command
     * @return the result of the command
     */
    private <T> T execute(String operation, java.util.function.Function<RedisCommands<String, String>, T> work) {
        try (StatefulRedisConnection<String, String> connection = connect()) {
            return work.apply(connection.sync());
        } catch (RedisException failure) {
            throw CatedraException.unavailable(
                    "The catalog cache of the catedra is not reachable (" + operation + " failed): "
                            + failure.getMessage());
        }
    }

    private StatefulRedisConnection<String, String> connect() {
        return client().connect();
    }

    private RedisClient client() {
        RedisClient current = client;
        if (current != null) {
            return current;
        }
        synchronized (mutex) {
            if (client == null) {
                client = build();
            }
            return client;
        }
    }

    /**
     * Builds the client from the externalized settings. No connection is opened here, so
     * calling this method cannot fail because the cache is down.
     */
    private RedisClient build() {
        CatedraProperties.Redis redis = properties.redis();
        io.lettuce.core.RedisURI uri = io.lettuce.core.RedisURI.builder()
                .withHost(redis.host())
                .withPort(redis.port())
                .withTimeout(COMMAND_TIMEOUT)
                .build();
        if (notBlank(redis.username())) {
            uri.setUsername(redis.username());
            uri.setPassword(emptyToNull(redis.password()));
        } else if (notBlank(redis.password())) {
            uri.setPassword(emptyToNull(redis.password()));
        }

        RedisClient created = RedisClient.create(uri);
        created.setOptions(ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .build())
                .build());
        return created;
    }

    @Override
    public void destroy() {
        RedisClient current = client;
        client = null;
        if (current != null) {
            try {
                current.close();
            } catch (RuntimeException alreadyClosed) {
                // The client is being shut down: nothing useful can be done about it.
            }
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * @return the command type of a raw Redis call; only used to document the surface used
     */
    static CommandType[] supportedCommands() {
        return new CommandType[]{CommandType.GET, CommandType.HGET, CommandType.HGETALL,
                CommandType.SET};
    }
}
