package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository;

import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository. The domain never imports this type: it is reached only through the
 * adapter of this package.
 */
public interface JpaProfessionalCategoryRepository extends JpaRepository<ProfessionalCategoryEntity, Long> {
}
