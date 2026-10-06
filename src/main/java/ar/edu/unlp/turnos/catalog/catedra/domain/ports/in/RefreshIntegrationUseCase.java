package ar.edu.unlp.turnos.catalog.catedra.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;

/**
 * Use case: obtain the technical token (when needed) and refresh the integration stored
 * locally with the value reported by the catedra.
 */
public interface RefreshIntegrationUseCase {

    /**
     * Fetches the integration from the catedra, validates that it is consistent with the
     * environment configuration and persists it.
     *
     * @return the refreshed integration, already persisted
     */
    CatedraIntegration refresh();
}
