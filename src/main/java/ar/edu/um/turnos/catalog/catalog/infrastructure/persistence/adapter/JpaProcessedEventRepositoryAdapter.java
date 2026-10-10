package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.adapter;

import ar.edu.um.turnos.catalog.catalog.domain.ports.out.ProcessedEventRepository;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProcessedEventEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.repository.JpaProcessedEventRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for the {@link ProcessedEventRepository} port.
 *
 * <p>{@link #markProcessed} is a multi-step operation (prune old entries, then insert the new
 * one) and therefore runs in a transaction: the eventId is either recorded together with its
 * effects or not at all.</p>
 *
 * <p>The ledger is pruned to a fixed retention window. Replaying an event older than that
 * window is harmless: applying the versions that are already applied produces the same
 * rows, and the eventId is then recorded again.</p>
 */
@Component
@RequiredArgsConstructor
public class JpaProcessedEventRepositoryAdapter implements ProcessedEventRepository {

    /** How long an eventId is remembered. Ample margin over any realistic redelivery window. */
    private static final Duration RETENTION = Duration.ofDays(30);

    private final JpaProcessedEventRepository processedEventRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId) {
        return processedEventRepository.existsById(eventId);
    }

    @Override
    @Transactional
    public void markProcessed(String eventId, Long catalogVersion, Instant processedAt) {
        processedEventRepository.deleteProcessedBefore(processedAt.minus(RETENTION));

        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setCatalogVersion(catalogVersion);
        entity.setProcessedAt(processedAt);
        processedEventRepository.save(entity);
    }
}
