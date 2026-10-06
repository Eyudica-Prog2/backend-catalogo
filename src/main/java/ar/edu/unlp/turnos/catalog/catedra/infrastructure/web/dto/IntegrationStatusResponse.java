package ar.edu.unlp.turnos.catalog.catedra.infrastructure.web.dto;

import java.time.Instant;

/**
 * Body of {@code GET /api/internal/integration-status}.
 *
 * <p>It intentionally omits {@code redisPassword} and the technical JWT: secrets are never
 * part of an HTTP response of this service.</p>
 */
public record IntegrationStatusResponse(
        String groupId,
        String provisioningStatus,
        String redisHost,
        Integer redisPort,
        String redisUsername,
        String redisReadNamespace,
        String redisWriteNamespace,
        String kafkaBootstrapServers,
        String kafkaConsumerGroupId,
        String kafkaCatalogTopic,
        String kafkaAppointmentActionsTopic,
        String kafkaAppointmentPhoneTopic,
        Instant lastRefreshedAt,
        Instant createdAt,
        Instant updatedAt) {
}
