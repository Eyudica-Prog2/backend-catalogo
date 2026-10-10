package ar.edu.um.turnos.catalog.catedra.infrastructure.rest.dto;

/**
 * {@code integration} object returned by the catedra (contract v1, sections 5.1 / 5.3).
 *
 * <p>Property names are exactly the ones of the contract; unknown extra fields are ignored
 * by the application mapper configuration, as required for forward compatibility.</p>
 *
 * <p>{@code redisPassword} is only delivered while {@code provisioningStatus} is
 * {@code PROVISIONED}.</p>
 */
public record IntegrationResponse(
        String groupId,
        String redisHost,
        Integer redisPort,
        String redisUsername,
        String redisPassword,
        String redisReadNamespace,
        String redisWriteNamespace,
        String kafkaBootstrapServers,
        String kafkaConsumerGroupId,
        String kafkaCatalogTopic,
        String kafkaAppointmentActionsTopic,
        String kafkaAppointmentPhoneTopic,
        String provisioningStatus) {
}
