package ar.edu.um.turnos.catalog.catalog.infrastructure.kafka.dto;

import java.time.Instant;

/**
 * Body of a {@code CatalogUpdated} notification as it arrives from Kafka (contract v1,
 * section 15.3).
 *
 * <p>Every field is nullable on purpose: the payload is read as it arrives and the rules
 * that define a valid event live in the domain model, so a missing field is reported with
 * its own message instead of a Jackson error.</p>
 *
 * @param eventId      idempotency key of the message
 * @param eventType    type of the event, {@code CatalogUpdated} for this topic
 * @param newVersion   version being announced
 * @param occurredAt   instant the event was produced, optional
 * @param source       origin of the version ({@code MANUAL}, {@code SCHEDULED}, ...), optional
 * @param schemaVersion contract schema of the payload
 */
public record CatalogUpdatedPayload(String eventId,
                                    String eventType,
                                    Long newVersion,
                                    Instant occurredAt,
                                    String source,
                                    Integer schemaVersion) {
}
