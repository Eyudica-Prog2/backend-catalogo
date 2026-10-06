package ar.edu.unlp.turnos.catalog.catedra.domain.ports.out;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import java.util.Optional;

/**
 * Outbound port: local persistence of the technical integration.
 *
 * <p>Only the domain model is exposed; entities, repositories and the encryption of the
 * redis password live behind the infrastructure adapter.</p>
 */
public interface IntegrationRepository {

    /**
     * @return the most recently stored integration, empty when nothing was stored yet
     */
    Optional<CatedraIntegration> findCurrent();

    /**
     * @param integration integration to store
     * @return the stored integration, including the local audit timestamps
     */
    CatedraIntegration save(CatedraIntegration integration);
}
