package ar.edu.unlp.turnos.catalog.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.kafka.CatalogUpdatedKafkaListener;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository.JpaProcessedEventRepository;
import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import ar.edu.unlp.turnos.catalog.support.CatedraRedisFixture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

/**
 * Consumption of {@code CatalogUpdated} from {@code catedra.catalog.{groupId}}
 * (sections 6 and 10.1 of the statement, section 18.1 of the reference).
 *
 * <p>The listener is invoked directly with exactly the body the broker would deliver: there
 * is no broker in the suite, so what is proven here is the behaviour of the consumer
 * (idempotency, tolerance of unknown fields, refusal of a payload that is not a v1 event and
 * the order between "effects persisted" and "eventId recorded"), which is the part that can
 * go wrong independently of the transport. The offset itself is committed by the container
 * after this method returns normally, with {@code enable-auto-commit=false} and record
 * acknowledgement.</p>
 */
class CatalogUpdatedKafkaTest extends CatalogApiIntegrationTest {

    @Autowired
    private CatalogUpdatedKafkaListener listener;

    @Autowired
    private JpaProcessedEventRepository processedEvents;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private CatalogSyncRepository syncRepository;

    @Test
    void theSameNotificationIsAppliedOnlyOnce() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        CatedraRedisFixture.publishWindow(4, 1);
        CatedraRedisFixture.publishProfessional(CatalogFixtures.ANA_PEREZ,
                CatalogFixtures.HEALTH_CATEGORY, "Ana", "PerezKafka", true);
        CatedraRedisFixture.publishChanges(4, CatalogFixtures.ANA_PEREZ);

        String payload = notification("event-0001", 4);

        // First delivery: the pending version is applied and only then the eventId is
        // recorded, so a crash between the two redelivers instead of hiding the message.
        listener.onCatalogUpdated(payload);
        assertThat(processedEvents.count()).isEqualTo(1L);
        assertThat(incrementalGateway.changesVersionsRequested()).containsExactly(4L);
        assertThat(professional(CatalogFixtures.ANA_PEREZ).getLastName()).isEqualTo("PerezKafka");

        // Second delivery of the same eventId: recognized before anything is touched.
        listener.onCatalogUpdated(payload);
        assertThat(processedEvents.count()).isEqualTo(1L);
        assertThat(incrementalGateway.changesVersionsRequested()).containsExactly(4L);
        assertThat(professional(CatalogFixtures.ANA_PEREZ).getLastName()).isEqualTo("PerezKafka");
        assertThat(appliedVersion()).isEqualTo(4L);
    }

    @Test
    void aPayloadWithUnknownFieldsInsideContractV1IsStillAccepted() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        // Nothing is pending: the notification only has to be recognized and recorded.
        CatedraRedisFixture.publishWindow(3, 1);

        listener.onCatalogUpdated("{\"eventId\":\"event-0002\",\"eventType\":\"CatalogUpdated\","
                + "\"newVersion\":3,\"schemaVersion\":1,\"source\":\"SCHEDULED\","
                + "\"occurredAt\":\"2026-07-01T10:00:00Z\",\"aFieldAddedInsideContractV1\":true}");

        assertThat(processedEvents.count()).isEqualTo(1L);
        assertThat(incrementalGateway.changesVersionsRequested()).isEmpty();
    }

    @Test
    void aPayloadThatIsNotAV1EventIsSkippedWithoutFailingTheConsumer() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());
        CatedraRedisFixture.publishWindow(3, 1);

        // Wrong type: it can never become valid, so it is logged and skipped instead of
        // being redelivered forever.
        listener.onCatalogUpdated("{\"eventId\":\"event-0003\",\"eventType\":\"SomethingElse\","
                + "\"newVersion\":3,\"schemaVersion\":1}");

        assertThat(processedEvents.count()).isZero();
        assertThat(appliedVersion()).isEqualTo(3L);
        assertThat(incrementalGateway.changesVersionsRequested()).isEmpty();
    }

    @Test
    void theEventIdIsOnlyRecordedAfterTheEffectsWerePersisted() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        // The delta of the announced version is not published: the application must fail,
        // and a failed application must not be remembered as processed.
        CatedraRedisFixture.publishWindow(4, 1);

        assertThatThrownBy(() -> listener.onCatalogUpdated(notification("event-0004", 4)))
                .isInstanceOf(CatedraException.class);

        assertThat(processedEvents.count()).isZero();
        assertThat(appliedVersion()).isEqualTo(3L);

        // The failure is visible through the status instead of being swallowed.
        mockMvc.perform(get("/api/sync/status").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(3))
                .andExpect(jsonPath("$.lastError").isNotEmpty());
    }

    private String notification(String eventId, long newVersion) {
        return "{\"eventId\":\"" + eventId + "\",\"eventType\":\"CatalogUpdated\","
                + "\"newVersion\":" + newVersion + ",\"schemaVersion\":1,"
                + "\"source\":\"MANUAL\",\"occurredAt\":\"2026-07-01T10:00:00Z\"}";
    }

    private Professional professional(long id) {
        return catalogRepository.findProfessionalById(id)
                .orElseThrow(() -> new AssertionError("professional " + id + " is not local"))
                .professional();
    }

    private long appliedVersion() {
        return syncRepository.findState()
                .orElseThrow(() -> new AssertionError("the local copy has no state"))
                .getAppliedVersion();
    }
}
