package ar.edu.um.turnos.catalog.catalog.domain.ports.in;

import java.util.List;

import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;

/**
 * Use case: list the professional categories of the local copy.
 */
public interface GetProfessionalCategoriesUseCase {

    /**
     * @return every category of the local copy, including the disabled ones, ordered by name
     */
    List<ProfessionalCategory> getProfessionalCategories();
}
