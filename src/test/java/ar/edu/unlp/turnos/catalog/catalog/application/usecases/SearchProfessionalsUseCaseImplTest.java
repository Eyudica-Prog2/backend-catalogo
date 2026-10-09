package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.SortDirection;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the use case that searches professionals over the local copy.
 *
 * <p>The use case must not reinterpret the criteria: the filters arrive ready to be executed
 * and the page it answers is exactly the one the port produced (total included).</p>
 */
@ExtendWith(MockitoExtension.class)
class SearchProfessionalsUseCaseImplTest {

    @Mock
    private CatalogRepository catalogRepository;

    @InjectMocks
    private SearchProfessionalsUseCaseImpl useCase;

    @Test
    void executesTheCriteriaItReceivedAndReturnsThatExactPage() {
        ProfessionalSearchCriteria criteria = new ProfessionalSearchCriteria(
                10L, "ana", true, true, 1, 20, "lastName", SortDirection.ASC);
        ProfessionalPage page = new ProfessionalPage(List.of(), 0L, 1, 20);
        when(catalogRepository.searchProfessionals(criteria)).thenReturn(page);

        assertThat(useCase.searchProfessionals(criteria)).isSameAs(page);

        verify(catalogRepository).searchProfessionals(criteria);
    }
}
