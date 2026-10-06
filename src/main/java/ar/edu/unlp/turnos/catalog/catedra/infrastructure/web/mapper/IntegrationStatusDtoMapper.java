package ar.edu.unlp.turnos.catalog.catedra.infrastructure.web.mapper;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.web.dto.IntegrationStatusResponse;
import org.springframework.stereotype.Component;

/**
 * Maps the domain model into the HTTP response of this service.
 *
 * <p>Only the fields listed in {@link IntegrationStatusResponse} are copied, so the redis
 * password cannot leak by accident: there is no code path that copies it.</p>
 */
@Component
public class IntegrationStatusDtoMapper {

    /**
     * @param integration stored integration
     * @return response DTO without any secret
     */
    public IntegrationStatusResponse toResponse(CatedraIntegration integration) {
        return new IntegrationStatusResponse(
                integration.getGroupId(),
                integration.getProvisioningStatus() == null
                        ? null
                        : integration.getProvisioningStatus().name(),
                integration.getRedisHost(),
                integration.getRedisPort(),
                integration.getRedisUsername(),
                integration.getRedisReadNamespace(),
                integration.getRedisWriteNamespace(),
                integration.getKafkaBootstrapServers(),
                integration.getKafkaConsumerGroupId(),
                integration.getKafkaCatalogTopic(),
                integration.getKafkaAppointmentActionsTopic(),
                integration.getKafkaAppointmentPhoneTopic(),
                integration.getLastRefreshedAt(),
                integration.getCreatedAt(),
                integration.getUpdatedAt());
    }
}
