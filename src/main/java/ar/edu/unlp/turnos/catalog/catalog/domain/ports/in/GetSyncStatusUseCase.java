package ar.edu.unlp.turnos.catalog.catalog.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;

/**
 * Use case: report the state of the synchronization (applied version, last success, last
 * error), as required by section 4.1 of the statement.
 */
public interface GetSyncStatusUseCase {

    /**
     * @return the current state; a service that never synchronized answers version zero
     *          with no error instead of failing
     */
    CatalogSyncState getSyncStatus();
}
