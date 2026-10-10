package ar.edu.um.turnos.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.um.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.um.turnos.catalog.catedra.domain.ports.out.IntegrationRepository;
import ar.edu.um.turnos.catalog.shared.config.AppJwtProperties;
import ar.edu.um.turnos.catalog.shared.config.CatedraProperties;
import ar.edu.um.turnos.catalog.shared.config.SecretsProperties;
import ar.edu.um.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.um.turnos.catalog.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test of the running application: one single {@code @SpringBootTest} for the whole
 * service, backed by a real PostgreSQL container (no H2 anywhere).
 *
 * <p>The catedra endpoint points to a closed port on purpose: the service must boot, expose
 * its health and keep answering even when the catedra is not available.</p>
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CatalogApplicationTests {

    private static final String TEST_JWT_SECRET = "test-jwt-secret-with-at-least-32-characters";
    private static final String TEST_SECRETS_KEY = "test-secrets-key-with-at-least-32-characters";
    private static final String REDIS_PASSWORD = "redis-password-from-catedra";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("app.jwt.expiry-seconds", () -> "3600");
        registry.add("app.jwt.remember-me-expiry-seconds", () -> "86400");
        registry.add("app.secrets.key", () -> TEST_SECRETS_KEY);
        registry.add("catedra.api.base-url", () -> "http://127.0.0.1:1");
        registry.add("catedra.api.login", () -> "technical-login");
        registry.add("catedra.api.password", () -> "technical-password");
        registry.add("catedra.http.connect-timeout-ms", () -> "250");
        registry.add("catedra.http.read-timeout-ms", () -> "250");
        registry.add("catedra.retry.max-attempts", () -> "1");
        registry.add("catedra.redis.host", () -> "redis.catedra.internal");
        registry.add("catedra.redis.port", () -> "6379");
        registry.add("catedra.redis.username", () -> "grp_proyecto-test");
        registry.add("catedra.redis.password", () -> REDIS_PASSWORD);
        registry.add("catedra.redis.read-namespace", () -> "catedra:sync:*");
        registry.add("catedra.redis.write-namespace", () -> "alumnos:proyecto-test:*");
        registry.add("catedra.kafka.bootstrap-servers", () -> "kafka.catedra.internal:9092");
        registry.add("catedra.kafka.consumer-group-id", () -> "alumnos-proyecto-test");
        registry.add("catedra.kafka.catalog-topic", () -> "catedra.catalog.proyecto-test");
        registry.add("catedra.kafka.actions-topic", () -> "alumnos.turnos.acciones.proyecto-test");
        registry.add("catedra.kafka.phone-topic", () -> "catedra.turnos.telefono.proyecto-test");
        // No broker and no timer: this class only proves the service boots and answers.
        registry.add("spring.kafka.bootstrap-servers", () -> "127.0.0.1:1");
        registry.add("app.sync.kafka.listener-enabled", () -> "false");
        registry.add("app.sync.scheduler.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationRepository integrationRepository;

    @Autowired
    private CatedraProperties catedraProperties;

    @Autowired
    private AppJwtProperties appJwtProperties;

    @Autowired
    private SecretsProperties secretsProperties;

    @Autowired
    private Environment environment;

    @Test
    void contextLoadsAndDataSourceComesFromTheEnvironment() {
        assertThat(environment.getProperty("spring.datasource.url")).startsWith("jdbc:postgresql://");
    }

    @Test
    void healthEndpointIsPublicAndAnswers200() throws Exception {
        mockMvc.perform(get("/api/internal/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("backend-catalogo"));
    }

    @Test
    void integrationStatusWithoutTokenIsRejectedWith401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/internal/integration-status"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void integrationStatusWithMalformedTokenIsRejectedWith401() throws Exception {
        mockMvc.perform(get("/api/internal/integration-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void integrationStatusReturnsStoredDataWithoutSecrets() throws Exception {
        integrationRepository.save(CatedraIntegration.builder()
                .groupId("proyecto-test")
                .redisHost("redis.catedra.internal")
                .redisPort(6379)
                .redisUsername("grp_proyecto-test")
                .redisPassword(REDIS_PASSWORD)
                .redisReadNamespace("catedra:sync:*")
                .redisWriteNamespace("alumnos:proyecto-test:*")
                .kafkaBootstrapServers("kafka.catedra.internal:9092")
                .kafkaConsumerGroupId("alumnos-proyecto-test")
                .kafkaCatalogTopic("catedra.catalog.proyecto-test")
                .kafkaAppointmentActionsTopic("alumnos.turnos.acciones.proyecto-test")
                .kafkaAppointmentPhoneTopic("catedra.turnos.telefono.proyecto-test")
                .provisioningStatus(ProvisioningStatus.PROVISIONED)
                .build());

        String body = mockMvc.perform(get("/api/internal/integration-status")
                        .header(HttpHeaders.AUTHORIZATION,
                                "Bearer " + TestTokens.hs256(TEST_JWT_SECRET, "test.user")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groupId").value("proyecto-test"))
                .andExpect(jsonPath("$.provisioningStatus").value("PROVISIONED"))
                .andExpect(jsonPath("$.redisHost").value("redis.catedra.internal"))
                .andExpect(jsonPath("$.redisPassword").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(REDIS_PASSWORD);
        assertThat(body).doesNotContain(TEST_JWT_SECRET);

        // The stored copy round-trips through the cipher: same clear value in, same value out.
        assertThat(integrationRepository.findCurrent())
                .hasValueSatisfying(stored -> assertThat(stored.getRedisPassword()).isEqualTo(REDIS_PASSWORD));
    }

    @Test
    void requiredConfigurationIsReadFromTheEnvironment() {
        assertThat(appJwtProperties.secret()).isEqualTo(TEST_JWT_SECRET);
        assertThat(appJwtProperties.expirySeconds()).isEqualTo(3600L);
        assertThat(secretsProperties.key()).isEqualTo(TEST_SECRETS_KEY);
        assertThat(catedraProperties.api().baseUrl()).isEqualTo("http://127.0.0.1:1");
        assertThat(catedraProperties.api().hasCredentials()).isTrue();
        assertThat(catedraProperties.redis().password()).isEqualTo(REDIS_PASSWORD);
        assertThat(catedraProperties.kafka().catalogTopic()).isEqualTo("catedra.catalog.proyecto-test");
        assertThat(catedraProperties.http().connectTimeoutMs()).isEqualTo(250L);
        assertThat(catedraProperties.retry().maxAttempts()).isEqualTo(1);
    }
}
