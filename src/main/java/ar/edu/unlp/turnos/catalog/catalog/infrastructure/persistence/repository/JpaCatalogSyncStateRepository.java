package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository;

import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository. The domain never imports this type: it is reached only through the
 * adapter of this package.
 */
public interface JpaCatalogSyncStateRepository extends JpaRepository<CatalogSyncStateEntity, Long> {

    /**
     * The service keeps a single row (enforced by a CHECK on the primary key), so the
     * earliest row is the current state.
     *
     * @return the only sync state row
     */
    Optional<CatalogSyncStateEntity> findFirstByOrderByIdAsc();
}
