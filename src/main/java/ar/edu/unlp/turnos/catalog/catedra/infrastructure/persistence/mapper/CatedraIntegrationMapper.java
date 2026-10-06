package ar.edu.unlp.turnos.catalog.catedra.infrastructure.persistence.mapper;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.persistence.entity.CatedraIntegrationEntity;
import org.springframework.stereotype.Component;

/**
 * Maps the domain model to the JPA entity and back.
 *
 * <p>The mapper is pure: encryption and decryption of the redis password are performed by the
 * adapter and passed in as plain values, so this class can be tested without a cipher.</p>
 */
@Component
public class CatedraIntegrationMapper {

    /**
     * @param entity         persisted row
     * @param redisPassword  already decrypted redis password, may be null
     * @return domain representation
     */
    public CatedraIntegration toDomain(CatedraIntegrationEntity entity, String redisPassword) {
        return CatedraIntegration.builder()
                .groupId(entity.getGroupId())
                .redisHost(entity.getRedisHost())
                .redisPort(entity.getRedisPort())
                .redisUsername(entity.getRedisUsername())
                .redisPassword(redisPassword)
                .redisReadNamespace(entity.getRedisReadNamespace())
                .redisWriteNamespace(entity.getRedisWriteNamespace())
                .kafkaBootstrapServers(entity.getKafkaBootstrapServers())
                .kafkaConsumerGroupId(entity.getKafkaConsumerGroupId())
                .kafkaCatalogTopic(entity.getKafkaCatalogTopic())
                .kafkaAppointmentActionsTopic(entity.getKafkaActionsTopic())
                .kafkaAppointmentPhoneTopic(entity.getKafkaPhoneTopic())
                .provisioningStatus(ProvisioningStatus.from(entity.getProvisioningStatus()))
                .lastRefreshedAt(entity.getLastRefreshedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * @param domain             domain model
     * @param encryptedPassword  already encrypted redis password, may be null
     * @return a brand new entity, ready to be persisted
     */
    public CatedraIntegrationEntity toEntity(CatedraIntegration domain, String encryptedPassword) {
        CatedraIntegrationEntity entity = new CatedraIntegrationEntity();
        updateEntity(entity, domain, encryptedPassword);
        return entity;
    }

    /**
     * Copies the domain values into an existing row. Audit timestamps are not touched: they
     * are owned by the JPA lifecycle callbacks.
     *
     * @param entity            target row
     * @param domain            domain model
     * @param encryptedPassword already encrypted redis password, may be null
     */
    public void updateEntity(CatedraIntegrationEntity entity, CatedraIntegration domain, String encryptedPassword) {
        entity.setGroupId(domain.getGroupId());
        entity.setRedisHost(domain.getRedisHost());
        entity.setRedisPort(domain.getRedisPort());
        entity.setRedisUsername(domain.getRedisUsername());
        entity.setRedisPasswordCiphertext(encryptedPassword);
        entity.setRedisReadNamespace(domain.getRedisReadNamespace());
        entity.setRedisWriteNamespace(domain.getRedisWriteNamespace());
        entity.setKafkaBootstrapServers(domain.getKafkaBootstrapServers());
        entity.setKafkaConsumerGroupId(domain.getKafkaConsumerGroupId());
        entity.setKafkaCatalogTopic(domain.getKafkaCatalogTopic());
        entity.setKafkaActionsTopic(domain.getKafkaAppointmentActionsTopic());
        entity.setKafkaPhoneTopic(domain.getKafkaAppointmentPhoneTopic());
        entity.setProvisioningStatus(domain.getProvisioningStatus() == null
                ? ProvisioningStatus.UNKNOWN.name()
                : domain.getProvisioningStatus().name());
    }
}
