package ar.edu.um.turnos.catalog.catalog.domain.ports.out;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;

/**
 * Outbound port: download the complete catalog snapshot from the catedra
 * ({@code GET /api/synchronization/snapshot}, contract v1, section 7).
 *
 * <p>The implementation owns everything that belongs to the remote call: technical
 * authentication, bounded retries, timeouts and the translation of the answer into the
 * domain model. The caller only sees a snapshot or an error with a stable functional code.</p>
 */
public interface CatalogSnapshotGateway {

    /**
     * @return the published snapshot, including disabled records
     * @throws RuntimeException when the catedra is unreachable, rejects the technical
     *                          credentials or answers with a payload that cannot be read;
     *                          the application layer exposes it with a stable functional code
     */
    CatalogSnapshot fetchCatalogSnapshot();
}
