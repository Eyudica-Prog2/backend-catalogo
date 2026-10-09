package ar.edu.unlp.turnos.catalog.catedra.application.usecases;

import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.in.GetIntegrationStatusUseCase;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
