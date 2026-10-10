package ar.edu.um.turnos.catalog.catalog.domain.ports.out;

import java.time.Instant;

/**
 * Outbound port: idempotency ledger of the catalog notifications (contract v1, section 18.1).
 *
 * <p>Kafka delivers at least once, so the same {@code eventId} can arrive more than once.
 * Recording it after the effects were persisted lets the consumer recognize a repeat, skip
 * it and confirm the offset without repeating anything.</p>
 */
public interface ProcessedEventRepository {

    /**
     * @param eventId idempotency key of the notification
     * @return true when that eventId was already applied by this service
     */
    boolean isProcessed(String eventId);

    /**
     * Records the eventId as processed and prunes the oldest entries of the ledger.
     *
     * <p>It must be called <strong>after</strong> the effects of the event were persisted:
     * the opposite order would let a crash hide an event that was never applied.</p>
     *
     * @param eventId        idempotency key of the notification
     * @param catalogVersion version announced by the event, {@code null} when unknown
     * @param processedAt    instant the event was applied
     */
    void markProcessed(String eventId, Long catalogVersion, Instant processedAt);
}
