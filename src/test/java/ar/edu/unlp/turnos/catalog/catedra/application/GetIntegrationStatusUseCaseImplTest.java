package ar.edu.unlp.turnos.catalog.catedra.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.catedra.application.usecases.GetIntegrationStatusUseCaseImpl;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the integration status use case.
 */
@ExtendWith(MockitoExtension.class)
class GetIntegrationStatusUseCaseImplTest {

    @Mock
    private IntegrationRepository repository;

    @InjectMocks
    private GetIntegrationStatusUseCaseImpl useCase;

    @Test
    void returnsTheStoredIntegration() {
        CatedraIntegration stored = IntegrationFixtures.integration("proyecto-abc");
        when(repository.findCurrent()).thenReturn(Optional.of(stored));

        assertThat(useCase.getIntegrationStatus()).isSameAs(stored);
    }

    @Test
    void answersNotFoundWhenNothingIsStoredYet() {
        when(repository.findCurrent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getIntegrationStatus())
                .isInstanceOfSatisfying(CatedraException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("CATEDRA_INTEGRATION_NOT_FOUND");
                    assertThat(exception.getHttpStatus()).isEqualTo(404);
                    assertThat(exception.getDetail()).isNotBlank();
                });
    }
}
