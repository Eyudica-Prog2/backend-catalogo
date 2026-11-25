package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;

/**
 * Spring Data repository. The domain never imports this type: it is reached only through the
 * adapter of this package.
 */
public interface JpaProfessionalCategoryRepository extends JpaRepository<ProfessionalCategoryEntity, Long> {
}
