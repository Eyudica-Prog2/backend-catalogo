package ar.edu.unlp.turnos.catalog.catalog.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;

/**
 * Use case: run a full synchronization, that is, download the snapshot from the catedra and
 * apply it to the local copy.
 *
 * <p>Used by {@code POST /api/internal/sync/force} and by the startup routine that
 * initializes an empty local copy.</p>
 */
public interface RunFullSyncUseCase {

    /**
     * Fetches and applies the snapshot. Failures of the download are recorded in the sync
     * state (without touching the local copy) and then propagated.
     *
     * @return the synchronization state after a successful synchronization
     * @throws RuntimeException {@code CATEDRA_*} when the catedra cannot be reached or
     *                          rejected the technical credentials, {@code 503
     *                          SNAPSHOT_INVALID} when the snapshot cannot be applied
     */
    CatalogSyncState runFullSync();
}
