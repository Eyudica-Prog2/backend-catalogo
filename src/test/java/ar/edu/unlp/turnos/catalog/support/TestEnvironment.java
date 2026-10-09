package ar.edu.unlp.turnos.catalog.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Environment of the integration tests: real PostgreSQL, a real Redis with the same key
 * names the catedra publishes, and the externalized configuration the service needs to boot.
 *
 * <p>Shared by the base class of the catalog tests and by the startup test, which runs with
 * its own container and its own context on purpose.</p>
 *
 * <p>Deliberate decisions:</p>
 *
 * <ul>
 *   <li>the catedra base url points to a closed port with one attempt, so a test that
 *       accidentally reaches the real client fails fast instead of hanging;</li>
 *   <li>the same applies to Redis: it runs in a container of this machine, so the incremental
 *       feed is read through the real adapter (namespaces, hashes, JSON and all) instead of
 *       through a mock that would agree with any implementation;</li>
 *   <li>the Kafka consumer and the periodic catch up are switched off: neither a broker nor a
 *       timer must move the state of a test between two assertions. The listener is exercised
 *       directly, with the same payload the broker would deliver.</li>
 * </ul>
 */
public final class TestEnvironment {

    /** HS256 secret of the end user JWT, used to mint valid tokens with {@link TestTokens}. */
    public static final String TEST_JWT_SECRET = "test-jwt-secret-with-at-least-32-characters";

    /** Key namespace the catedra publishes for this project, section 14 of the contract. */
    public static final String READ_NAMESPACE = "catedra:sync:*";

    /** Private namespace of this project for the diagnostic of the last attempt. */
    public static final String WRITE_NAMESPACE = "alumnos:proyecto-test:*";

    /** Docker image of Redis; already present in the machine that runs the suite. */
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    private static final String TEST_SECRETS_KEY = "test-secrets-key-with-at-least-32-characters";

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

    private TestEnvironment() {
    }

    /**
     * Starts the containers when they are not running yet and registers every property the
     * service reads from the environment.
     *
     * @param registry  registry of the {@code @DynamicPropertySource} method
     * @param postgres  container that backs the test
     */
    public static void register(DynamicPropertyRegistry registry, PostgreSQLContainer<?> postgres) {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        startRedis();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.jwt.expiry-seconds", () -> "3600");
        registry.add("app.jwt.remember-me-expiry-seconds", () -> "86400");
        registry.add("app.secrets.key", () -> TEST_SECRETS_KEY);
        registry.add("catedra.api.base-url", () -> "http://127.0.0.1:1");
        registry.add("catedra.api.login", () -> "technical-login");
        registry.add("catedra.api.password", () -> "technical-password");
        registry.add("catedra.http.connect-timeout-ms", () -> "250");
        registry.add("catedra.http.read-timeout-ms", () -> "250");
        registry.add("catedra.retry.max-attempts", () -> "1");
        registry.add("catedra.redis.host", REDIS::getHost);
        registry.add("catedra.redis.port", () -> String.valueOf(REDIS.getMappedPort(6379)));
        registry.add("catedra.redis.username", () -> "");
        registry.add("catedra.redis.password", () -> "");
        registry.add("catedra.redis.read-namespace", () -> READ_NAMESPACE);
        registry.add("catedra.redis.write-namespace", () -> WRITE_NAMESPACE);
        registry.add("catedra.kafka.bootstrap-servers", () -> "127.0.0.1:1");
        registry.add("catedra.kafka.consumer-group-id", () -> "alumnos-proyecto-test");
        registry.add("catedra.kafka.catalog-topic", () -> "catedra.catalog.proyecto-test");
        registry.add("catedra.kafka.actions-topic", () -> "alumnos.turnos.acciones.proyecto-test");
        registry.add("catedra.kafka.phone-topic", () -> "catedra.turnos.telefono.proyecto-test");
        // Spring Kafka never connects: there is no broker and the listener must not start.
        registry.add("spring.kafka.bootstrap-servers", () -> "127.0.0.1:1");
        registry.add("spring.kafka.consumer.group-id", () -> "alumnos-proyecto-test");
        registry.add("app.sync.kafka.listener-enabled", () -> "false");
        // No timer may move the state of a test between two assertions.
        registry.add("app.sync.scheduler.enabled", () -> "false");
    }

    /**
     * @return the Redis container shared by every context of this JVM, already running
     */
    public static GenericContainer<?> redis() {
        startRedis();
        return REDIS;
    }

    private static synchronized void startRedis() {
        if (!REDIS.isRunning()) {
            REDIS.start();
        }
    }
}
