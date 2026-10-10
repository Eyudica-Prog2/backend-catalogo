package ar.edu.um.turnos.catalog.catalog.application.usecases;

import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.GetProfessionalCategoriesUseCase;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Lists the categories of the local copy. Disabled categories are returned too: they are a
 * logical deletion of the catedra and the client decides what to show with them.
 */
@Component
@RequiredArgsConstructor
public class GetProfessionalCategoriesUseCaseImpl implements GetProfessionalCategoriesUseCase {

    private final CatalogRepository catalogRepository;

    @Override
    public List<ProfessionalCategory> getProfessionalCategories() {
        return catalogRepository.findAllCategories();
    }
}
