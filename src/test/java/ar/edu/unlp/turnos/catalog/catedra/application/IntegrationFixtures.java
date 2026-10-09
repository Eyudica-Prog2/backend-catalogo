package ar.edu.unlp.turnos.catalog.catedra.application;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.ProvisioningStatus;

/**
 * Small factory shared by the tests of the {@code catedra} application package.
 */
public final class IntegrationFixtures {

    private IntegrationFixtures() {
    }

    /**
     * @param groupId technical group of the project
     * @return a complete, contract compliant integration
     */
    public static CatedraIntegration integration(String groupId) {
        return CatedraIntegration.builder()
                .groupId(groupId)
                .redisHost("redis.catedra.internal")
                .redisPort(6379)
                .redisUsername("grp_" + groupId)
                .redisPassword("redis-secret")
                .redisReadNamespace("catedra:sync:*")
                .redisWriteNamespace("alumnos:" + groupId + ":*")
                .kafkaBootstrapServers("kafka.catedra.internal:9092")
                .kafkaConsumerGroupId("alumnos-" + groupId)
                .kafkaCatalogTopic("catedra.catalog." + groupId)
                .kafkaAppointmentActionsTopic("alumnos.turnos.acciones." + groupId)
                .kafkaAppointmentPhoneTopic("catedra.turnos.telefono." + groupId)
                .provisioningStatus(ProvisioningStatus.PROVISIONED)
                .build();
    }
}
