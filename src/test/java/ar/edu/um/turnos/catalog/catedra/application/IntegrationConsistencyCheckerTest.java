package ar.edu.um.turnos.catalog.catedra.application;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.um.turnos.catalog.shared.config.CatedraProperties;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the environment is validated against the catedra response, and that the redis
 * password is never part of a warning.
 */
class IntegrationConsistencyCheckerTest {

    private static final String REDIS_PASSWORD = "super-secret-redis-password";

    private final CatedraProperties properties = new CatedraProperties(
            new CatedraProperties.Api("http://catedra.test", "login", "password", ""),
            new CatedraProperties.Http(1000, 1000),
            new CatedraProperties.Retry(3, 100, 1000),
            new CatedraProperties.Redis("redis.catedra.internal", 6379, "grp_proyecto-abc", REDIS_PASSWORD,
                    "catedra:sync:*", "alumnos:proyecto-abc:*"),
            new CatedraProperties.Kafka("kafka.catedra.internal:9092", "alumnos-proyecto-abc",
                    "catedra.catalog.proyecto-abc", "alumnos.turnos.acciones.proyecto-abc",
                    "catedra.turnos.telefono.proyecto-abc"));

    private final IntegrationConsistencyChecker checker = new IntegrationConsistencyChecker(properties);

    @Test
    void returnsNoWarningsWhenEverythingMatches() {
        CatedraIntegration integration = IntegrationFixtures.integration("proyecto-abc");

        List<String> warnings = checker.check(integration, null);

        assertThat(warnings).isEmpty();
    }

    @Test
    void warnsWhenAnEnvironmentValueIsStale() {
        CatedraIntegration integration = IntegrationFixtures.integration("proyecto-abc");
        CatedraIntegration outdated = integration.toBuilder()
                .kafkaCatalogTopic("catedra.catalog.proyecto-viejo")
                .build();

        List<String> warnings = checker.check(outdated, null);

        assertThat(warnings)
                .anySatisfy(warning -> assertThat(warning).contains("CATEDRA_KAFKA_CATALOG_TOPIC"))
                .anySatisfy(warning -> assertThat(warning).contains("catedra.catalog.proyecto-viejo"));
        assertThat(warnings).noneSatisfy(warning -> assertThat(warning).contains(REDIS_PASSWORD));
    }

    @Test
    void warnsWhenProvisioningIsNotCompleted() {
        CatedraIntegration pending = IntegrationFixtures.integration("proyecto-abc").toBuilder()
                .provisioningStatus(ProvisioningStatus.PENDING)
                .build();

        List<String> warnings = checker.check(pending, null);

        assertThat(warnings)
                .anySatisfy(warning -> assertThat(warning).contains("PENDING"))
                .noneSatisfy(warning -> assertThat(warning).contains(REDIS_PASSWORD));
    }

    @Test
    void warnsWhenTheGroupIdChanged() {
        CatedraIntegration current = IntegrationFixtures.integration("proyecto-nuevo");
        CatedraIntegration previous = IntegrationFixtures.integration("proyecto-viejo");

        List<String> warnings = checker.check(current, previous);

        assertThat(warnings).anySatisfy(warning -> {
            assertThat(warning).contains("proyecto-viejo");
            assertThat(warning).contains("proyecto-nuevo");
        });
    }

    @Test
    void neverReportsTheRedisPassword() {
        CatedraIntegration mismatch = IntegrationFixtures.integration("proyecto-abc").toBuilder()
                .redisHost("redis-otro-host")
                .kafkaCatalogTopic("otro-topico")
                .build();

        List<String> warnings = checker.check(mismatch, null);

        assertThat(warnings).isNotEmpty();
        assertThat(warnings).noneSatisfy(warning -> assertThat(warning).contains(REDIS_PASSWORD));
        assertThat(warnings.toString()).doesNotContain(REDIS_PASSWORD);
    }
}
