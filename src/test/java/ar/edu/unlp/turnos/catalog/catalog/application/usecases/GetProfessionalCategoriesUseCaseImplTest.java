package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the use case that lists the categories of the local copy: it returns what the
 * port answers, disabled categories included, without filtering anything here.
 */
@ExtendWith(MockitoExtension.class)
class GetProfessionalCategoriesUseCaseImplTest {

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private GetProfessionalCategoriesUseCaseImpl useCase;

    @Test
    void returnsEveryCategoryOfTheLocalCopyIncludingTheDisabledOnes() {
        List<ProfessionalCategory> categories = List.of(
                ProfessionalCategory.builder().id(10L).name("Salud").enabled(true).build(),
                ProfessionalCategory.builder().id(20L).name("Deporte").enabled(false).build());
        when(catalogRepository.findAllCategories()).thenReturn(categories);

        assertThat(useCase.getProfessionalCategories())
                .isEqualTo(categories)
                .extracting(ProfessionalCategory::isEnabled)
                .containsExactly(true, false);

        verify(catalogRepository).findAllCategories();
    }
}
