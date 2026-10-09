package ar.edu.unlp.turnos.catalog.catalog.domain.ports.out;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: read access to the local copy of the catalog.
 *
 * <p>Only domain types are exposed; entities, specifications and repositories stay behind
 * the infrastructure adapter. Writing the local copy is a different capability and belongs
 * to {@link CatalogSyncRepository}.</p>
 */
public interface CatalogRepository {

    /**
     * @return every category of the local copy, ordered by name
     */
    List<ProfessionalCategory> findAllCategories();

    /**
     * @param criteria filters, pagination and order
     * @return the requested page plus the total number of matches
     */
    ProfessionalPage searchProfessionals(ProfessionalSearchCriteria criteria);

    /**
     * @param professionalId remote id of the professional
     * @return the professional with all its weekly schedules, empty when it does not exist
     */
    Optional<ProfessionalDetails> findProfessionalById(Long professionalId);
}
