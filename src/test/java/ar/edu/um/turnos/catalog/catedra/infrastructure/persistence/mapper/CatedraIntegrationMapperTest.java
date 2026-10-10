package ar.edu.um.turnos.catalog.catedra.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.um.turnos.catalog.catedra.infrastructure.persistence.entity.CatedraIntegrationEntity;
import org.junit.jupiter.api.Test;

/**
 * Mapper tests: round trip, nulls and edge cases, with no Spring and no cipher involved.
 */
class CatedraIntegrationMapperTest {

    private final CatedraIntegrationMapper mapper = new CatedraIntegrationMapper();

    @Test
    void roundTripsDomainToEntityAndBack() {
        CatedraIntegration domain = CatedraIntegration.builder()
                .groupId("proyecto-abc")
                .redisHost("redis.catedra.internal")
                .redisPort(6379)
                .redisUsername("grp_proyecto-abc")
                .redisPassword("redis-password")
                .redisReadNamespace("catedra:sync:*")
                .redisWriteNamespace("alumnos:proyecto-abc:*")
                .kafkaBootstrapServers("kafka.catedra.internal:9092")
                .kafkaConsumerGroupId("alumnos-proyecto-abc")
                .kafkaCatalogTopic("catedra.catalog.proyecto-abc")
                .kafkaAppointmentActionsTopic("alumnos.turnos.acciones.proyecto-abc")
                .kafkaAppointmentPhoneTopic("catedra.turnos.telefono.proyecto-abc")
                .provisioningStatus(ProvisioningStatus.PROVISIONED)
                .build();

        CatedraIntegrationEntity entity = mapper.toEntity(domain, "encrypted-redis-password");
        CatedraIntegration restored = mapper.toDomain(entity, entity.getRedisPasswordCiphertext());

        assertThat(restored.getGroupId()).isEqualTo("proyecto-abc");
        assertThat(restored.getRedisHost()).isEqualTo("redis.catedra.internal");
        assertThat(restored.getRedisPort()).isEqualTo(6379);
        assertThat(restored.getRedisUsername()).isEqualTo("grp_proyecto-abc");
        assertThat(restored.getRedisReadNamespace()).isEqualTo("catedra:sync:*");
        assertThat(restored.getRedisWriteNamespace()).isEqualTo("alumnos:proyecto-abc:*");
        assertThat(restored.getKafkaBootstrapServers()).isEqualTo("kafka.catedra.internal:9092");
        assertThat(restored.getKafkaConsumerGroupId()).isEqualTo("alumnos-proyecto-abc");
        assertThat(restored.getKafkaCatalogTopic()).isEqualTo("catedra.catalog.proyecto-abc");
        assertThat(restored.getKafkaAppointmentActionsTopic()).isEqualTo("alumnos.turnos.acciones.proyecto-abc");
        assertThat(restored.getKafkaAppointmentPhoneTopic()).isEqualTo("catedra.turnos.telefono.proyecto-abc");
        assertThat(restored.getProvisioningStatus()).isEqualTo(ProvisioningStatus.PROVISIONED);
        // The mapper does not encrypt: it only moves the values it was given.
        assertThat(restored.getRedisPassword()).isEqualTo("encrypted-redis-password");
    }

    @Test
    void keepsTheEncryptedPasswordOutOfTheDomainMappingWhenItIsMissing() {
        CatedraIntegration domain = CatedraIntegration.builder()
                .groupId("proyecto-abc")
                .redisHost("redis.catedra.internal")
                .redisPort(6379)
                .kafkaBootstrapServers("kafka.catedra.internal:9092")
                .provisioningStatus(ProvisioningStatus.PENDING)
                .build();

        CatedraIntegrationEntity entity = mapper.toEntity(domain, null);

        assertThat(entity.getRedisPasswordCiphertext()).isNull();
        assertThat(entity.getProvisioningStatus()).isEqualTo("PENDING");
        assertThat(mapper.toDomain(entity, null).getRedisPassword()).isNull();
    }

    @Test
    void mapsUnknownProvisioningStatusWithoutFailing() {
        CatedraIntegrationEntity entity = new CatedraIntegrationEntity();
        entity.setGroupId("proyecto-abc");
        entity.setProvisioningStatus("SOMETHING_NEW");

        assertThat(mapper.toDomain(entity, null).getProvisioningStatus()).isEqualTo(ProvisioningStatus.UNKNOWN);
    }

    @Test
    void updatesAnExistingRowWithoutTouchingTheAuditColumns() {
        CatedraIntegrationEntity entity = new CatedraIntegrationEntity();
        entity.setGroupId("proyecto-viejo");
        entity.setCreatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        entity.setUpdatedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));

        mapper.updateEntity(entity, CatedraIntegration.builder()
                .groupId("proyecto-nuevo")
                .redisHost("redis.catedra.internal")
                .redisPort(6379)
                .kafkaBootstrapServers("kafka.catedra.internal:9092")
                .provisioningStatus(ProvisioningStatus.PROVISIONED)
                .build(), "cipher");

        assertThat(entity.getGroupId()).isEqualTo("proyecto-nuevo");
        assertThat(entity.getCreatedAt()).isEqualTo(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(entity.getUpdatedAt()).isEqualTo(java.time.Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void domainRejectsBlankGroupId() {
        assertThatThrownBy(() -> CatedraIntegration.builder().groupId("  ").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("groupId");
    }
}
