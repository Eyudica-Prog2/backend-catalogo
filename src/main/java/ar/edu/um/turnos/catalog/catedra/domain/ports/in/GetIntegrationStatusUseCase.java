package ar.edu.um.turnos.catalog.catedra.domain.ports.in;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;

/**
 * Use case: read the technical integration currently stored by this service.
 */
public interface GetIntegrationStatusUseCase {

    /**
     * @return the stored integration
     * @throws RuntimeException when no integration was stored yet; the application layer maps
     *                          that case to the functional code {@code CATEDRA_INTEGRATION_NOT_FOUND}
     */
    CatedraIntegration getIntegrationStatus();
}
