package ar.edu.um.turnos.catalog.catalog.domain.ports.in;

import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import java.util.List;

/**
 * Use case: list the professional categories of the local copy.
 */
public interface GetProfessionalCategoriesUseCase {

    /**
     * @return every category of the local copy, including the disabled ones, ordered by name
     */
    List<ProfessionalCategory> getProfessionalCategories();
}
