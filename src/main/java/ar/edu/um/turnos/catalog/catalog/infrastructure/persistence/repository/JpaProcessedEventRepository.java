package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.repository;

import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProcessedEventEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository behind {@code processed_event}. It stays inside the
 * infrastructure: the domain only knows the {@code ProcessedEventRepository} port.
 */
public interface JpaProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {

    /**
     * Prunes the entries that can no longer be redelivered, keeping the ledger bounded.
     *
     * @param threshold instant before which entries are removed
     * @return how many entries were removed
     */
    @Modifying
    @Query("delete from ProcessedEventEntity e where e.processedAt < :threshold")
    int deleteProcessedBefore(@Param("threshold") Instant threshold);
}
