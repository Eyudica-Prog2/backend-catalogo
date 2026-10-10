package ar.edu.um.turnos.catalog.catedra.application.usecases;

import ar.edu.um.turnos.catalog.catedra.application.IntegrationConsistencyChecker;
import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.ports.in.RefreshIntegrationUseCase;
import ar.edu.um.turnos.catalog.catedra.domain.ports.out.CatedraIntegrationGateway;
import ar.edu.um.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Refreshes the locally stored technical integration.
 *
 * <p>Responsibilities: ask the catedra (via the outbound port), compare the answer with the
 * environment configuration, report inconsistencies as warnings and persist the result.
 * There is no facade above this use case: the controller injects the use case ports directly,
 * because a facade would only delegate.</p>
 */
@Component
@RequiredArgsConstructor
public class RefreshIntegrationUseCaseImpl implements RefreshIntegrationUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshIntegrationUseCaseImpl.class);

    private final CatedraIntegrationGateway gateway;
    private final IntegrationRepository repository;
    private final IntegrationConsistencyChecker consistencyChecker;

    @Override
    public CatedraIntegration refresh() {
        CatedraIntegration fetched = gateway.fetchCurrentIntegration();

        Optional<CatedraIntegration> previouslyStored = repository.findCurrent();
        List<String> warnings = consistencyChecker.check(fetched, previouslyStored.orElse(null));
        warnings.forEach(warning -> log.warn("Catedra integration consistency warning: {}", warning));

        CatedraIntegration stored = repository.save(fetched);
        log.info("Catedra integration stored: groupId={}, provisioningStatus={}, lastRefreshedAt={}",
                stored.getGroupId(), stored.getProvisioningStatus(), stored.getLastRefreshedAt());
        return stored;
    }
}
