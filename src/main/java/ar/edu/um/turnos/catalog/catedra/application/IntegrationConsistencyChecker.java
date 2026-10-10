package ar.edu.um.turnos.catalog.catedra.application;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.um.turnos.catalog.shared.config.CatedraProperties;
import ar.edu.um.turnos.catalog.shared.util.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates that the integration reported by the catedra is coherent with the values that
 * this project configured in its environment ({@code .env}).
 *
 * <p>The catedra is the authoritative source, so inconsistencies never abort the refresh:
 * they produce warnings that tell the operator which environment variable is stale. The
 * redis password is never compared, logged or returned.</p>
 */
@Component
@RequiredArgsConstructor
public class IntegrationConsistencyChecker {

    private final CatedraProperties properties;

    /**
     * @param integration       integration returned by the catedra
     * @param previouslyStored  integration stored before this refresh, may be null
     * @return immutable list of warnings; empty when everything matches
     */
    public List<String> check(CatedraIntegration integration, CatedraIntegration previouslyStored) {
        List<String> warnings = new ArrayList<>();

        if (integration.getProvisioningStatus() != ProvisioningStatus.PROVISIONED) {
            warnings.add("provisioningStatus is " + integration.getProvisioningStatus()
                    + "; the catedra only delivers the redis password while the account is PROVISIONED.");
        }
        if (Strings.isBlank(integration.getRedisHost())) {
            warnings.add("The catedra reported an empty redis host.");
        }
        if (integration.getRedisPort() < 1 || integration.getRedisPort() > 65535) {
            warnings.add("The catedra reported an out of range redis port: " + integration.getRedisPort() + ".");
        }
        if (Strings.isBlank(integration.getKafkaBootstrapServers())) {
            warnings.add("The catedra reported an empty kafka bootstrap servers value.");
        }
        if (Strings.isNotBlank(integration.getGroupId())
                && Strings.isNotBlank(integration.getRedisWriteNamespace())
                && !integration.getRedisWriteNamespace().contains(integration.getGroupId())) {
            warnings.add("The redis write namespace does not contain the group id.");
        }

        CatedraProperties.Redis redis = properties.redis();
        CatedraProperties.Kafka kafka = properties.kafka();
        compareAgainstEnvironment(warnings, "CATEDRA_REDIS_HOST", redis.host(), integration.getRedisHost());
        compareAgainstEnvironment(warnings, "CATEDRA_REDIS_USERNAME", redis.username(), integration.getRedisUsername());
        compareAgainstEnvironment(warnings, "CATEDRA_REDIS_READ_NAMESPACE", redis.readNamespace(),
                integration.getRedisReadNamespace());
        compareAgainstEnvironment(warnings, "CATEDRA_REDIS_WRITE_NAMESPACE", redis.writeNamespace(),
                integration.getRedisWriteNamespace());
        compareAgainstEnvironment(warnings, "CATEDRA_KAFKA_BOOTSTRAP_SERVERS", kafka.bootstrapServers(),
                integration.getKafkaBootstrapServers());
        compareAgainstEnvironment(warnings, "CATEDRA_KAFKA_CONSUMER_GROUP_ID", kafka.consumerGroupId(),
                integration.getKafkaConsumerGroupId());
        compareAgainstEnvironment(warnings, "CATEDRA_KAFKA_CATALOG_TOPIC", kafka.catalogTopic(),
                integration.getKafkaCatalogTopic());
        compareAgainstEnvironment(warnings, "CATEDRA_KAFKA_ACTIONS_TOPIC", kafka.actionsTopic(),
                integration.getKafkaAppointmentActionsTopic());
        compareAgainstEnvironment(warnings, "CATEDRA_KAFKA_PHONE_TOPIC", kafka.phoneTopic(),
                integration.getKafkaAppointmentPhoneTopic());

        if (previouslyStored != null
                && !Objects.equals(previouslyStored.getGroupId(), integration.getGroupId())) {
            warnings.add("The group id changed from '" + previouslyStored.getGroupId() + "' to '"
                    + integration.getGroupId() + "'; the environment configuration looks stale.");
        }

        return List.copyOf(warnings);
    }

    private void compareAgainstEnvironment(List<String> warnings, String variableName,
                                           String environmentValue, String catedraValue) {
        if (Strings.isBlank(environmentValue) || Strings.isBlank(catedraValue)) {
            return;
        }
        if (!environmentValue.trim().equals(catedraValue.trim())) {
            warnings.add(variableName + " from the environment (" + environmentValue
                    + ") differs from the catedra response (" + catedraValue + ").");
        }
    }
}
