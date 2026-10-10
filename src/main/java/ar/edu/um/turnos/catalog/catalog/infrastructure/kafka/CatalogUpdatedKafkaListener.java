package ar.edu.um.turnos.catalog.catalog.infrastructure.kafka;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogEventOutcome;
import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogUpdatedEvent;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.ProcessCatalogUpdatedUseCase;
import ar.edu.um.turnos.catalog.catalog.infrastructure.kafka.dto.CatalogUpdatedPayload;
import ar.edu.um.turnos.catalog.catalog.infrastructure.kafka.mapper.CatalogUpdatedPayloadMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter that consumes {@code CatalogUpdated} from
 * {@code catedra.catalog.{groupId}} (contract v1, section 15.3).
 *
 * <p>Decisions worth documenting:</p>
 *
 * <ul>
 *   <li><strong>the event only notifies</strong>: this class never carries catalog data. It
 *       turns the payload into a domain event and delegates to
 *       {@link ProcessCatalogUpdatedUseCase}, which reads the real data from Redis;</li>
 *   <li><strong>offset is committed after the work is persisted</strong>: the container runs
 *       with {@code enable-auto-commit=false} and record acknowledgement, so the offset of a
 *       record is committed only once this method returns normally. A failure therefore
 *       leaves the record uncommitted and Kafka redelivers it;</li>
 *   <li><strong>unknown fields are ignored</strong>: the payload is read with the shared
 *       {@link ObjectMapper}, which does not fail on unknown properties, so new optional
 *       fields inside contract v1 do not break this consumer;</li>
 *   <li><strong>a payload that is not a valid v1 event is logged and skipped</strong>
 *       instead of throwing: it can never become valid, and retrying it would hold the
 *       partition forever. The log states the reason, never the whole payload;</li>
 *   <li><strong>a failure while applying the event does propagate</strong>, so Kafka
 *       redelivers it with a bounded backoff. If it still fails, the periodic catch-up
 *       (section 16) applies the pending versions later: no infinite retry loop.</li>
 * </ul>
 *
 * <p>The listener can be disabled with {@code app.sync.kafka.listener-enabled=false}, which
 * is what the automated tests do: they invoke {@link #onCatalogUpdated(String)} directly
 * against a real database and no broker.</p>
 */
@Component
@RequiredArgsConstructor
public class CatalogUpdatedKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogUpdatedKafkaListener.class);

    private final ObjectMapper objectMapper;
    private final CatalogUpdatedPayloadMapper payloadMapper;
    private final ProcessCatalogUpdatedUseCase processCatalogUpdated;

    /**
     * Consumes one notification.
     *
     * <p>The method is deliberately public and free of Kafka types so a test can hand it a
     * payload exactly as the broker would.</p>
     *
     * @param payload raw JSON body of the message
     */
    @KafkaListener(topics = "${catedra.kafka.catalog-topic}",
            groupId = "${catedra.kafka.consumer-group-id}",
            autoStartup = "${app.sync.kafka.listener-enabled}")
    public void onCatalogUpdated(String payload) {
        CatalogUpdatedEvent event = parse(payload);
        if (event == null) {
            return;
        }
        CatalogEventOutcome outcome = processCatalogUpdated.process(event);
        log.info("Catalog notification handled: eventId={}, newVersion={}, outcome={}",
                event.getEventId(), event.getNewVersion(), outcome);
    }

    /**
     * @param payload raw JSON body of the message
     * @return the validated event, or {@code null} when the payload is not a valid v1 event
     *         (the caller then skips the record and lets the offset be committed)
     */
    private CatalogUpdatedEvent parse(String payload) {
        try {
            CatalogUpdatedPayload parsed = objectMapper.readValue(payload, CatalogUpdatedPayload.class);
            return payloadMapper.toDomain(parsed);
        } catch (JsonProcessingException | IllegalArgumentException rejected) {
            // The body is never logged: only the reason, so a malformed or unexpected
            // payload cannot flood the logs nor expose anything it may contain.
            log.warn("Skipping a message of the catalog topic that is not a valid CatalogUpdated "
                    + "event (contract v1, section 15.3): {}", rejected.getMessage());
            return null;
        }
    }
}
