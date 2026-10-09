package ar.edu.unlp.turnos.catalog.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that puts a {@link StubCatalogSnapshotGateway} in the context.
 *
 * <p>It is listed explicitly in {@code @SpringBootTest(classes = ...)} by every integration
 * test of the catalog: the stub is {@code @Primary}, so any component that needs a snapshot
 * gets it instead of the real adapter, which would perform an HTTP call against the catedra.</p>
 */
@TestConfiguration
public class StubCatalogSnapshotConfiguration {

    @Bean
    @Primary
    public StubCatalogSnapshotGateway stubCatalogSnapshotGateway() {
        return new StubCatalogSnapshotGateway();
    }
}
