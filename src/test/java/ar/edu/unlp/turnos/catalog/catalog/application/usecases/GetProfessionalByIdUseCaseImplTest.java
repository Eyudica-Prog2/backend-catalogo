package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests of the use case that reads one professional of the local copy.
 */
@ExtendWith(MockitoExtension.class)
class GetProfessionalByIdUseCaseImplTest {

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private GetProfessionalByIdUseCaseImpl useCase;

    @Test
    void returnsTheProfessionalWithItsWeeklySchedules() {
        Professional professional = Professional.builder()
                .id(CatalogFixtures.ANA_PEREZ)
                .categoryId(CatalogFixtures.HEALTH_CATEGORY)
                .firstName("Ana")
                .lastName("Perez")
                .enabled(true)
                .build();
        ProfessionalDetails details = new ProfessionalDetails(professional,
                List.of(CatalogFixtures.snapshot(1).getWeeklySchedules().get(0)));
        when(catalogRepository.findProfessionalById(CatalogFixtures.ANA_PEREZ))
                .thenReturn(Optional.of(details));

        assertThat(useCase.getProfessionalById(CatalogFixtures.ANA_PEREZ)).isSameAs(details);
    }

    @Test
    void answersNotFoundWhenTheLocalCopyDoesNotContainTheProfessional() {
        when(catalogRepository.findProfessionalById(999999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getProfessionalById(999999L))
                .isInstanceOfSatisfying(CatalogException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PROFESSIONAL_NOT_FOUND");
                    assertThat(exception.getHttpStatus()).isEqualTo(404);
                    assertThat(exception.getDetail()).isNotBlank();
                });
    }
}
