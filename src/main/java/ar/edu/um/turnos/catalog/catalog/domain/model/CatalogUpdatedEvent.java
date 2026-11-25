package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * {@code CatalogUpdated} notification received from {@code catedra.catalog.{groupId}}
 * (contract v1, section 15.3).
 *
 * <p>The event only <em>announces</em> a new version: the data always comes from Redis or
 * from a snapshot. {@code eventId} is the idempotency key of the message and
 * {@code newVersion} is also the message key.</p>
 *
 * <p>Pure domain model. Its invariants are enforced by the constructor, so a payload that
 * does not describe a valid v1 event is rejected before it reaches the use case.</p>
 */
@Getter
@Builder(toBuilder = true)
public class CatalogUpdatedEvent {

    /** Only event type this service consumes from the catalog topic. */
    public static final String EVENT_TYPE = "CatalogUpdated";

    /** Only schema version accepted inside contract v1. */
    public static final int SCHEMA_VERSION = 1;

    private final String eventId;
    private final String eventType;
    private final long newVersion;
    private final Instant occurredAt;
    private final String source;
    private final int schemaVersion;

    /**
     * @param eventId      idempotency key of the message, must not be blank
     * @param eventType    must be exactly {@link #EVENT_TYPE}
     * @param newVersion   version being announced, must be positive
     * @param occurredAt   instant the event was produced, may be null
     * @param source       origin of the version ({@code MANUAL}, {@code SCHEDULED}, ...), may be null
     * @param schemaVersion contract schema of the payload, must be exactly {@link #SCHEMA_VERSION}
     * @throws IllegalArgumentException when the payload does not describe a valid event
     */
    public CatalogUpdatedEvent(String eventId, String eventType, long newVersion,
                               Instant occurredAt, String source, int schemaVersion) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("A CatalogUpdated event must declare an eventId.");
        }
        if (!EVENT_TYPE.equals(eventType)) {
            throw new IllegalArgumentException(
                    "Expected eventType=" + EVENT_TYPE + " but received eventType=" + eventType + ".");
        }
        if (schemaVersion != SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Expected schemaVersion=" + SCHEMA_VERSION + " but received schemaVersion="
                            + schemaVersion + ".");
        }
        if (newVersion <= 0) {
            throw new IllegalArgumentException(
                    "A CatalogUpdated event must announce a positive newVersion, received "
                            + newVersion + ".");
        }
        this.eventId = eventId;
        this.eventType = eventType;
        this.newVersion = newVersion;
        this.occurredAt = occurredAt;
        this.source = source;
        this.schemaVersion = schemaVersion;
    }
}
