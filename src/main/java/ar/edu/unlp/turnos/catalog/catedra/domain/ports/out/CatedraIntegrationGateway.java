package ar.edu.unlp.turnos.catalog.catedra.domain.ports.out;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;

/**
 * Outbound port: read the current technical integration from the catedra.
 *
 * <p>The implementation owns everything that belongs to the remote call: technical
 * authentication, bounded retries, timeouts and the translation of the catedra errors.</p>
 */
public interface CatedraIntegrationGateway {

    /**
     * @return the integration reported by the catedra
     * @throws RuntimeException when the catedra cannot be reached, rejects the credentials,
     *                          answers with an unexpected payload or returns an error of its
     *                          own; the application layer exposes it as a functional error code
     */
    CatedraIntegration fetchCurrentIntegration();
}
