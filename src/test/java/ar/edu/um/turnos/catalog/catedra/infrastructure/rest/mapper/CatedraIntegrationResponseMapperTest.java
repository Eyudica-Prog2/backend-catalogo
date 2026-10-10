package ar.edu.um.turnos.catalog.catedra.infrastructure.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.um.turnos.catalog.catedra.infrastructure.rest.dto.IntegrationResponse;
import org.junit.jupiter.api.Test;

/**
 * Mapper unit test: contract payload to domain, including nulls and edge cases, with no HTTP
 * client and no Spring context involved.
 */
class CatedraIntegrationResponseMapperTest {

    private final CatedraIntegrationResponseMapper mapper = new CatedraIntegrationResponseMapper();

    @Test
    void mapsEveryContractFieldIntoTheDomain() {
        CatedraIntegration domain = mapper.toDomain(payload());

        assertThat(domain.getGroupId()).isEqualTo("proyecto-abc");
        assertThat(domain.getRedisHost()).isEqualTo("redis.catedra.internal");
        assertThat(domain.getRedisPort()).isEqualTo(6379);
        assertThat(domain.getRedisUsername()).isEqualTo("grp_proyecto-abc");
        assertThat(domain.getRedisPassword()).isEqualTo("redis-secret");
        assertThat(domain.getRedisReadNamespace()).isEqualTo("catedra:sync:*");
        assertThat(domain.getRedisWriteNamespace()).isEqualTo("alumnos:proyecto-abc:*");
        assertThat(domain.getKafkaBootstrapServers()).isEqualTo("kafka.catedra.internal:9092");
        assertThat(domain.getKafkaConsumerGroupId()).isEqualTo("alumnos-proyecto-abc");
        assertThat(domain.getKafkaCatalogTopic()).isEqualTo("catedra.catalog.proyecto-abc");
        assertThat(domain.getKafkaAppointmentActionsTopic()).isEqualTo("alumnos.turnos.acciones.proyecto-abc");
        assertThat(domain.getKafkaAppointmentPhoneTopic()).isEqualTo("catedra.turnos.telefono.proyecto-abc");
        assertThat(domain.getProvisioningStatus()).isEqualTo(ProvisioningStatus.PROVISIONED);
    }

    @Test
    void treatsAProvisioningStatusOutsideTheContractAsUnknown() {
        IntegrationResponse response = new IntegrationResponse("proyecto-abc", "redis", 6379, null,
                null, "catedra:sync:*", "alumnos:proyecto-abc:*", "kafka:9092", null, null, null, null,
                "SOMETHING_NEW");

        assertThat(mapper.toDomain(response).getProvisioningStatus()).isEqualTo(ProvisioningStatus.UNKNOWN);
    }

    @Test
    void toleratesNullOptionalFieldsButRejectsEmptyPayloads() {
        IntegrationResponse response = new IntegrationResponse("proyecto-abc", "redis", null, null, null,
                null, null, "kafka:9092", null, null, null, null, null);

        CatedraIntegration domain = mapper.toDomain(response);
        assertThat(domain.getRedisPort()).isZero();
        assertThat(domain.getRedisPassword()).isNull();
        assertThat(domain.getProvisioningStatus()).isEqualTo(ProvisioningStatus.UNKNOWN);

        assertThatThrownBy(() -> mapper.toDomain(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPayloadsWithoutTheMinimumIdentificationData() {
        assertThat(mapper.isUsable(payload())).isTrue();
        assertThat(mapper.isUsable(null)).isFalse();
        assertThat(mapper.isUsable(new IntegrationResponse(null, "redis", 6379, null, null,
                null, null, "kafka:9092", null, null, null, null, "PROVISIONED"))).isFalse();
        assertThat(mapper.isUsable(new IntegrationResponse("proyecto-abc", " ", 6379, null, null,
                null, null, null, null, null, null, null, "PROVISIONED"))).isFalse();
    }

    private static IntegrationResponse payload() {
        return new IntegrationResponse("proyecto-abc", "redis.catedra.internal", 6379, "grp_proyecto-abc",
                "redis-secret", "catedra:sync:*", "alumnos:proyecto-abc:*", "kafka.catedra.internal:9092",
                "alumnos-proyecto-abc", "catedra.catalog.proyecto-abc", "alumnos.turnos.acciones.proyecto-abc",
                "catedra.turnos.telefono.proyecto-abc", "PROVISIONED");
    }
}
