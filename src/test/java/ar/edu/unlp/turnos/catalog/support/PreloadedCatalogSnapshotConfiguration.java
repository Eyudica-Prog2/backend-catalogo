package ar.edu.unlp.turnos.catalog.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration that puts a {@link StubCatalogSnapshotGateway} already loaded with a
 * valid snapshot.
 *
 * <p>It is used by the startup test: at boot the initialization routine finds an available
 * catedra that publishes {@link CatalogFixtures#snapshot(long)}, so the test can prove that
 * an empty local copy is filled during startup without any endpoint being called.</p>
 */
@TestConfiguration
public class PreloadedCatalogSnapshotConfiguration {

    @Bean
    @Primary
    public StubCatalogSnapshotGateway preloadedSnapshotGateway() {
        StubCatalogSnapshotGateway gateway = new StubCatalogSnapshotGateway();
        gateway.returns(CatalogFixtures.snapshot(7));
        return gateway;
    }
}
