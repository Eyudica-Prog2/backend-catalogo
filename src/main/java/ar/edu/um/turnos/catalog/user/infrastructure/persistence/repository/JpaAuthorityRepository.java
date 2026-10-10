package ar.edu.um.turnos.catalog.user.infrastructure.persistence.repository;

import ar.edu.um.turnos.catalog.user.infrastructure.persistence.entity.AuthorityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository of {@code jhi_authority} (lookup table of roles).
 */
public interface JpaAuthorityRepository extends JpaRepository<AuthorityEntity, String> {
}
