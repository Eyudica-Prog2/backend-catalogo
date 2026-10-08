package ar.edu.unlp.turnos.catalog.catalog.infrastructure.kafka.mapper;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogUpdatedEvent;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.kafka.dto.CatalogUpdatedPayload;
import org.springframework.stereotype.Component;

/**
 * Maps a {@code CatalogUpdated} payload into its domain event (contract v1, section 15.3).
 *
 * <p>Fields that the domain cannot represent (a missing {@code newVersion} or
 * {@code schemaVersion}) are reported as an {@link IllegalArgumentException} with a self
 * contained message; everything else, including a field added inside contract v1, is passed
 * through and the rules of the event itself decide whether the payload is acceptable.</p>
 */
@Component
public class CatalogUpdatedPayloadMapper {

    /**
     * @param payload payload received from Kafka, never null
     * @return the equivalent domain event
     * @throws IllegalArgumentException when a mandatory field is missing or invalid
     */
    public CatalogUpdatedEvent toDomain(CatalogUpdatedPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("The catalog notification payload is empty.");
        }
        if (payload.newVersion() == null) {
            throw new IllegalArgumentException("A CatalogUpdated event must declare a newVersion.");
        }
        if (payload.schemaVersion() == null) {
            throw new IllegalArgumentException("A CatalogUpdated event must declare a schemaVersion.");
        }
        return CatalogUpdatedEvent.builder()
                .eventId(payload.eventId())
                .eventType(payload.eventType())
                .newVersion(payload.newVersion())
                .occurredAt(payload.occurredAt())
                .source(payload.source())
                .schemaVersion(payload.schemaVersion())
                .build();
    }
}
