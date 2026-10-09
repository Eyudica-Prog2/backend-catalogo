package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.GetSyncStatusUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogIncrementalGateway;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Reports the state of the synchronization.
 *
 * <p>A service that never synchronized answers version zero with no error instead of
 * {@code 404}: the client must be able to show "pending" as a normal state.</p>
 *
 * <p>The version window ({@code currentVersion}/{@code oldestAvailableVersion}) is refreshed
 * from Redis on every read so it is never older than the last publication. When Redis is
 * unreachable the endpoint still answers with the window observed during the last
 * synchronization attempt, because an operator asking for the status while the cache is down
 * needs the local picture, not a {@code 503}.</p>
 */
@Component
@RequiredArgsConstructor
public class GetSyncStatusUseCaseImpl implements GetSyncStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(GetSyncStatusUseCaseImpl.class);

    private final CatalogSyncRepository syncRepository;
    private final CatalogIncrementalGateway incrementalGateway;

    @Override
    public CatalogSyncState getSyncStatus() {
        CatalogSyncState stored = syncRepository.findState().orElseGet(CatalogSyncState::notSynced);
        try {
            CatalogVersionWindow window = incrementalGateway.fetchVersionWindow();
            return stored.toBuilder()
                    .currentVersion(window.getCurrentVersion())
                    .oldestAvailableVersion(window.getOldestAvailableVersion())
                    .build();
        } catch (RuntimeException redisUnavailable) {
            log.debug("Reporting the last observed version window: Redis is not reachable ({})",
                    redisUnavailable.getMessage());
            return stored;
        }
    }
}
