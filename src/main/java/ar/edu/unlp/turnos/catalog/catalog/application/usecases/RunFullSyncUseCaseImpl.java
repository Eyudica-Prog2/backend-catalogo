package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.ApplySnapshotUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunFullSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSnapshotGateway;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.shared.error.ApiException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs a full synchronization: downloads the snapshot from the catedra and applies it.
 *
 * <p>Two different failures are recorded differently, because they mean different things for
 * the operator:</p>
 * <ul>
 *   <li>the download failed: no version was received, the applied version and the last known
 *       snapshot version stay untouched and only the error is recorded;</li>
 *   <li>the snapshot arrived but could not be applied: {@link ApplySnapshotUseCaseImpl}
 *       records the attempted version and rejects it, keeping the applied version.</li>
 * </ul>
 *
 * <p>In both cases the local copy keeps serving the data it already had.</p>
 */
@Component
@RequiredArgsConstructor
public class RunFullSyncUseCaseImpl implements RunFullSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(RunFullSyncUseCaseImpl.class);

    private static final String DOWNLOAD_FAILED =
            "The catalog snapshot could not be downloaded from the catedra.";

    private final CatalogSnapshotGateway snapshotGateway;
    private final ApplySnapshotUseCase applySnapshot;
    private final CatalogSyncRepository syncRepository;

    @Override
    public CatalogSyncState runFullSync() {
        CatalogSnapshot snapshot;
        try {
            snapshot = snapshotGateway.fetchCatalogSnapshot();
        } catch (ApiException failure) {
            recordFailure(failure.getDetail());
            throw failure;
        } catch (RuntimeException failure) {
            recordFailure(DOWNLOAD_FAILED);
            throw failure;
        }
        return applySnapshot.apply(snapshot);
    }

    /**
     * Keeps a failed download visible through {@code GET /api/sync/status} without hiding
     * the original error behind a problem of the state table.
     */
    private void recordFailure(String error) {
        try {
            syncRepository.recordFailure(null, error, Instant.now());
        } catch (RuntimeException recordingFailed) {
            log.warn("The failed synchronization could not be recorded: {}", recordingFailed.getMessage());
        }
    }
}
