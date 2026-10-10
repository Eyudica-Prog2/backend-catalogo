package ar.edu.um.turnos.catalog.catedra.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.catalog.shared.error.CatedraException;
import ar.edu.um.turnos.catalog.catedra.application.usecases.RefreshIntegrationUseCaseImpl;
import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.ports.out.CatedraIntegrationGateway;
import ar.edu.um.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the refresh use case: the outbound ports are mocked, no Spring context.
 */
@ExtendWith(MockitoExtension.class)
class RefreshIntegrationUseCaseImplTest {

    @Mock
    private CatedraIntegrationGateway gateway;

    @Mock
    private IntegrationRepository repository;

    @Mock
    private IntegrationConsistencyChecker consistencyChecker;

    @InjectMocks
    private RefreshIntegrationUseCaseImpl useCase;

    @Test
    void storesTheIntegrationReturnedByTheCatedra() {
        CatedraIntegration fetched = IntegrationFixtures.integration("proyecto-abc");
        when(gateway.fetchCurrentIntegration()).thenReturn(fetched);
        when(repository.findCurrent()).thenReturn(Optional.empty());
        when(consistencyChecker.check(fetched, null)).thenReturn(List.of());
        when(repository.save(fetched)).thenReturn(fetched);

        CatedraIntegration stored = useCase.refresh();

        assertThat(stored).isSameAs(fetched);
        verify(repository).save(fetched);
    }

    @Test
    void comparesTheFetchedIntegrationWithThePreviouslyStoredOne() {
        CatedraIntegration fetched = IntegrationFixtures.integration("proyecto-abc");
        CatedraIntegration previous = IntegrationFixtures.integration("proyecto-anterior");
        when(gateway.fetchCurrentIntegration()).thenReturn(fetched);
        when(repository.findCurrent()).thenReturn(Optional.of(previous));
        when(consistencyChecker.check(fetched, previous)).thenReturn(List.of());
        when(repository.save(fetched)).thenReturn(fetched);

        useCase.refresh();

        verify(consistencyChecker).check(fetched, previous);
    }

    @Test
    void reportsConsistencyWarningsWithoutFailing() {
        CatedraIntegration fetched = IntegrationFixtures.integration("proyecto-abc");
        List<String> warnings = List.of("CATEDRA_REDIS_HOST from the environment differs from the catedra response.");
        when(gateway.fetchCurrentIntegration()).thenReturn(fetched);
        when(repository.findCurrent()).thenReturn(Optional.empty());
        when(consistencyChecker.check(eq(fetched), any())).thenReturn(warnings);
        when(repository.save(fetched)).thenReturn(fetched);

        assertThat(useCase.refresh()).isSameAs(fetched);
        verify(repository).save(fetched);
    }

    @Test
    void doesNotPersistAnythingWhenTheCatedraFails() {
        CatedraException failure = CatedraException.unavailable("The catedra API is not reachable.");
        when(gateway.fetchCurrentIntegration()).thenThrow(failure);

        assertThatThrownBy(useCase::refresh).isSameAs(failure);

        verify(repository, never()).save(any());
        verify(consistencyChecker, never()).check(any(), any());
    }
}
