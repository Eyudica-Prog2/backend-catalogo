package ar.edu.unlp.turnos.catalog.catedra.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Technical integration of this project with the catedra service.
 *
 * <p>Pure domain model: no JPA, no Spring, no DTOs. It mirrors the {@code integration}
 * object of contract v1 plus the local audit timestamps.</p>
 *
 * <p>{@code redisPassword} is held in clear text only while the value travels from the catedra
 * response to the persistence adapter, which encrypts it before storing.</p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class CatedraIntegration {

    private String groupId;
    private String redisHost;
    private int redisPort;
    private String redisUsername;
    private String redisPassword;
    private String redisReadNamespace;
    private String redisWriteNamespace;
    private String kafkaBootstrapServers;
    private String kafkaConsumerGroupId;
    private String kafkaCatalogTopic;
    private String kafkaAppointmentActionsTopic;
    private String kafkaAppointmentPhoneTopic;
    private ProvisioningStatus provisioningStatus;
    private Instant lastRefreshedAt;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * All-args constructor used by the builder.
     *
     * <p>Domain invariant: an integration without a group id cannot isolate anything, so the
     * model rejects a blank one no matter which entry point builds it (HTTP, use case, test).</p>
     *
     * @param groupId technical group of this project at the catedra, must not be blank
     */
    public CatedraIntegration(String groupId, String redisHost, int redisPort, String redisUsername,
                              String redisPassword, String redisReadNamespace, String redisWriteNamespace,
                              String kafkaBootstrapServers, String kafkaConsumerGroupId,
                              String kafkaCatalogTopic, String kafkaAppointmentActionsTopic,
                              String kafkaAppointmentPhoneTopic, ProvisioningStatus provisioningStatus,
                              Instant lastRefreshedAt, Instant createdAt, Instant updatedAt) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId must not be blank.");
        }
        this.groupId = groupId;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.redisUsername = redisUsername;
        this.redisPassword = redisPassword;
        this.redisReadNamespace = redisReadNamespace;
        this.redisWriteNamespace = redisWriteNamespace;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
        this.kafkaConsumerGroupId = kafkaConsumerGroupId;
        this.kafkaCatalogTopic = kafkaCatalogTopic;
        this.kafkaAppointmentActionsTopic = kafkaAppointmentActionsTopic;
        this.kafkaAppointmentPhoneTopic = kafkaAppointmentPhoneTopic;
        this.provisioningStatus = provisioningStatus;
        this.lastRefreshedAt = lastRefreshedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
