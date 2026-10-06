package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.mapper;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto.IntegrationResponse;
import ar.edu.unlp.turnos.catalog.shared.util.Strings;
import org.springframework.stereotype.Component;

/**
 * Maps the contract payload of the catedra into the domain model.
 *
 * <p>This mapper never crosses layers: it only knows the external DTO and the domain type.
 * It fails fast when the payload does not identify an integration, which the adapter turns
 * into {@code CATEDRA_INVALID_RESPONSE}.</p>
 */
@Component
public class CatedraIntegrationResponseMapper {

    /**
     * @param response payload received from the catedra, never null
     * @return domain representation
     * @throws IllegalArgumentException when the payload does not identify an integration
     */
    public CatedraIntegration toDomain(IntegrationResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("The catedra integration payload is empty.");
        }
        return CatedraIntegration.builder()
                .groupId(response.groupId())
                .redisHost(response.redisHost())
                .redisPort(response.redisPort() == null ? 0 : response.redisPort())
                .redisUsername(response.redisUsername())
                .redisPassword(response.redisPassword())
                .redisReadNamespace(response.redisReadNamespace())
                .redisWriteNamespace(response.redisWriteNamespace())
                .kafkaBootstrapServers(response.kafkaBootstrapServers())
                .kafkaConsumerGroupId(response.kafkaConsumerGroupId())
                .kafkaCatalogTopic(response.kafkaCatalogTopic())
                .kafkaAppointmentActionsTopic(response.kafkaAppointmentActionsTopic())
                .kafkaAppointmentPhoneTopic(response.kafkaAppointmentPhoneTopic())
                .provisioningStatus(ProvisioningStatus.from(response.provisioningStatus()))
                .build();
    }

    /**
     * @param response payload received from the catedra
     * @return true when the payload carries the minimum data required by the contract
     */
    public boolean isUsable(IntegrationResponse response) {
        return response != null
                && Strings.isNotBlank(response.groupId())
                && Strings.isNotBlank(response.redisHost())
                && Strings.isNotBlank(response.kafkaBootstrapServers());
    }
}
