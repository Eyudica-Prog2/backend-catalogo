package ar.edu.um.turnos.catalog.support;

import ar.edu.um.turnos.catalog.catalog.infrastructure.redis.RedisCatalogIncrementalGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that decorates the real incremental gateway with a
 * {@link RecordingCatalogIncrementalGateway}.
 *
 * <p>Listed explicitly in {@code @SpringBootTest(classes = ...)} by the integration tests of
 * the catalog. The real adapter still performs every read against the Redis container: only
 * the trace of those reads is added, so a test can assert that a key was <em>not</em> read
 * (which is what a snapshot fallback that skips the missing delta must show).</p>
 */
@TestConfiguration
public class RecordingSyncConfiguration {

    @Bean
    @Primary
    public RecordingCatalogIncrementalGateway recordingCatalogIncrementalGateway(
            RedisCatalogIncrementalGateway realGateway) {
        return new RecordingCatalogIncrementalGateway(realGateway);
    }
}
