package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository;

import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Spring Data repository. The domain never imports this type: it is reached only through the
 * adapter of this package.
 *
 * <p>{@link JpaSpecificationExecutor} is what allows the adapter to combine the four
 * mandatory filters (category, name, enabled state and availability) in a single query.</p>
 */
public interface JpaProfessionalRepository
        extends JpaRepository<ProfessionalEntity, Long>, JpaSpecificationExecutor<ProfessionalEntity> {
}
