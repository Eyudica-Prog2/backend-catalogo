package ar.edu.um.turnos.catalog.catedra.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.ports.in.GetIntegrationStatusUseCase;
import ar.edu.um.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import ar.edu.um.turnos.catalog.shared.error.CatedraException;

/**
 * Reads the stored technical integration and translates the empty case into the functional
 * error {@code CATEDRA_INTEGRATION_NOT_FOUND} (HTTP 404).
 */
@Component
@RequiredArgsConstructor
public class GetIntegrationStatusUseCaseImpl implements GetIntegrationStatusUseCase {

    private final IntegrationRepository repository;

    @Override
    public CatedraIntegration getIntegrationStatus() {
        return repository.findCurrent().orElseThrow(CatedraException::integrationNotFound);
    }
}
