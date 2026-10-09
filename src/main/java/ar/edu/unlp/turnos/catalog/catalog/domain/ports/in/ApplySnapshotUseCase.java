package ar.edu.unlp.turnos.catalog.catalog.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;

/**
 * Use case: apply a complete catalog snapshot to the local copy.
 *
 * <p>The three collections and the applied version are written in a single transaction: the
 * version only advances when everything was applied (contract v1, section 7).</p>
 */
public interface ApplySnapshotUseCase {

    /**
     * Validates the snapshot and replaces the local copy with it.
     *
     * @param snapshot snapshot to apply, already fetched from the catedra
     * @return the synchronization state after the snapshot was applied
     * @throws RuntimeException {@code 503 SNAPSHOT_INVALID} when the snapshot does not match
     *                          the contract; the failed attempt is recorded in the sync state
     *                          without advancing the applied version
     */
    CatalogSyncState apply(CatalogSnapshot snapshot);
}
