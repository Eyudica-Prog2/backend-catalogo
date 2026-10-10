package ar.edu.um.turnos.catalog.catalog.application.usecases;

import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.SearchProfessionalsUseCase;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Searches professionals over the local copy.
 *
 * <p>The four mandatory filters (category, name, enabled state and availability) are applied
 * by the outbound port in a single query; this use case keeps the rule that "available"
 * means "professional with at least one enabled weekly schedule", which is the decision
 * documented in the iteration: the real occupation is resolved by {@code backend-turnos}
 * with the occupancies of the catedra.</p>
 */
@Component
@RequiredArgsConstructor
public class SearchProfessionalsUseCaseImpl implements SearchProfessionalsUseCase {

    private final CatalogRepository catalogRepository;

    @Override
    public ProfessionalPage searchProfessionals(ProfessionalSearchCriteria criteria) {
        return catalogRepository.searchProfessionals(criteria);
    }
}
