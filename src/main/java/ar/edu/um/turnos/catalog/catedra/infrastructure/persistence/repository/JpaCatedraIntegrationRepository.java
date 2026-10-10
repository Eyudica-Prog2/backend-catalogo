package ar.edu.um.turnos.catalog.catedra.infrastructure.persistence.repository;

import ar.edu.um.turnos.catalog.catedra.infrastructure.persistence.entity.CatedraIntegrationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository. The domain never imports this type: it is reached only through the
 * adapter of this package.
 */
public interface JpaCatedraIntegrationRepository extends JpaRepository<CatedraIntegrationEntity, Long> {

    /**
     * The service keeps a single integration row, so the latest one is the current one.
     *
     * @return the most recently created integration row
     */
    Optional<CatedraIntegrationEntity> findFirstByOrderByIdDesc();
}
