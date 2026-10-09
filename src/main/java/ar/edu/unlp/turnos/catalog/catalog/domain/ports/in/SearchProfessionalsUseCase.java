package ar.edu.unlp.turnos.catalog.catalog.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;

/**
 * Use case: search professionals over the local copy of the catalog.
 *
 * <p>All the mandatory filters of the statement (category, name, enabled state and
 * availability) are resolved here, against local data only: this service never queries the
 * catedra to answer a search (section 6.2 of the statement).</p>
 */
public interface SearchProfessionalsUseCase {

    /**
     * @param criteria filters, pagination and order
     * @return the requested page plus the total number of matches
     */
    ProfessionalPage searchProfessionals(ProfessionalSearchCriteria criteria);
}
