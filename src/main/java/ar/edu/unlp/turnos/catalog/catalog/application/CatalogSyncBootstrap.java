package ar.edu.unlp.turnos.catalog.catalog.application;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunFullSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.shared.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initializes an empty local copy during startup.
 *
 * <p>"Empty local copy" is detected as "no snapshot was ever applied": the three tables are
 * written exclusively by {@link RunFullSyncUseCase} and the applied version is stored in the
 * same transaction, so both conditions are equivalent by construction.</p>
 *
 * <p>It reads the state through the outbound port instead of through the status use case:
 * the status use case refreshes the version window from Redis, and the startup must not
 * wait for a cache that may be unreachable.</p>
 *
 * <p>The service must boot even when the catedra is unreachable or not configured yet, so
 * every failure is logged (never as fatal) and never aborts the startup: searches keep
 * answering with whatever the local copy already contains.</p>
 */
@Component
@RequiredArgsConstructor
public class CatalogSyncBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncBootstrap.class);

    private final CatalogSyncRepository syncRepository;
    private final RunFullSyncUseCase runFullSync;

    @Override
    public void run(ApplicationArguments args) {
        CatalogSyncState current = syncRepository.findState().orElseGet(CatalogSyncState::notSynced);
        if (current.isSynced()) {
            log.info("Local copy of the catalog already initialized: appliedVersion={}",
                    current.getAppliedVersion());
            return;
        }
        try {
            CatalogSyncState synced = runFullSync.runFullSync();
            log.info("Initial catalog synchronization completed: appliedVersion={}, lastSyncAt={}",
                    synced.getAppliedVersion(), synced.getLastSyncAt());
        } catch (ApiException failure) {
            log.warn("Initial catalog synchronization failed: code={}, status={}, detail={}",
                    failure.getCode(), failure.getHttpStatus(), failure.getDetail());
        } catch (RuntimeException failure) {
            log.error("Unexpected error during the initial catalog synchronization: {}",
                    failure.getMessage(), failure);
        }
    }
}
