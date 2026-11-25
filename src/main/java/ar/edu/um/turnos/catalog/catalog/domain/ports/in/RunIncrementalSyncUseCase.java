package ar.edu.um.turnos.catalog.catalog.domain.ports.in;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSyncState;

/**
 * Applies the catalog versions that are pending in Redis, in order, starting from the
 * version currently stored by this service.
 *
 * <p>When the local copy cannot be continued safely (no local version yet, a version
 * outside the published window or a local version newer than the published one) this case
 * does not guess: it falls back to a complete snapshot, which is the only way to rebuild a
 * consistent copy (contract v1, section 18.2).</p>
 */
public interface RunIncrementalSyncUseCase {

    /**
     * Reads the published window, applies every pending version in order and returns the
     * state of the synchronization.
     *
     * <p>Applying the same version twice does not change the outcome: each version reads
     * the current state of the affected ids and advances the local version only when all of
     * its changes were written in the same transaction.</p>
     *
     * @return the state after the attempt, including the observed version window
     * @throws RuntimeException when the window or a version cannot be read, when a version
     *                          can no longer be reconstructed (a snapshot is then required)
     *                          or when the fallback snapshot itself fails; the failure is
     *                          recorded so it is visible through {@code GET /api/sync/status}
     */
    CatalogSyncState runIncrementalSync();
}
