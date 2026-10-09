package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.ProvisioningStatus;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.mapper.CatedraIntegrationResponseMapper;
import ar.edu.unlp.turnos.catalog.shared.config.CatedraProperties;
import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.shared.technical.CatedraTechnicalClient;
import ar.edu.unlp.turnos.catalog.shared.technical.CatedraTokenHolder;
import ar.edu.unlp.turnos.catalog.shared.technical.RetryExecutor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Contract test of the catedra client against a local HTTP server (MockWebServer): happy
 * path, retries, authentication failures, re-authentication and error propagation.
 *
 * <p>The HTTP client is built exactly as {@code CatedraClientConfig} does, only the base URL
 * points to the local stub.</p>
 */
class CatedraIntegrationGatewayAdapterTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    void authenticatesAndFetchesTheIntegration() throws Exception {
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(200, integrationJson()));

        CatedraIntegration result = adapter(3, true).fetchCurrentIntegration();

        assertThat(result.getGroupId()).isEqualTo("proyecto-abc");
        assertThat(result.getProvisioningStatus()).isEqualTo(ProvisioningStatus.PROVISIONED);
        assertThat(result.getRedisPassword()).isEqualTo("redis-secret");
        assertThat(server.getRequestCount()).isEqualTo(2);

        RecordedRequest authentication = server.takeRequest();
        assertThat(authentication.getMethod()).isEqualTo("POST");
        assertThat(authentication.getPath()).isEqualTo("/api/authenticate");
        assertThat(authentication.getBody().readUtf8())
                .contains("\"username\":\"tech-login\"")
                .contains("\"rememberMe\":true");

        RecordedRequest integration = server.takeRequest();
        assertThat(integration.getMethod()).isEqualTo("GET");
        assertThat(integration.getPath()).isEqualTo("/api/student/integration");
        assertThat(integration.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer technical-token-1");
    }

    @Test
    void reusesTheTokenBetweenRefreshes() throws Exception {
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(200, integrationJson()));
        server.enqueue(json(200, integrationJson()));

        CatedraIntegrationGatewayAdapter client = adapter(3, true);
        client.fetchCurrentIntegration();
        client.fetchCurrentIntegration();

        assertThat(server.getRequestCount()).isEqualTo(3);
        server.takeRequest();
        server.takeRequest();
        RecordedRequest secondIntegration = server.takeRequest();
        assertThat(secondIntegration.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer technical-token-1");
    }

    @Test
    void answersCatedraAuthFailedWhenTheCredentialsAreRejectedWithoutRetrying() {
        server.enqueue(json(401,
                "{\"status\":401,\"title\":\"Unauthorized\",\"detail\":\"Bad credentials\",\"code\":\"UNAUTHORIZED\"}"));

        CatedraException exception = catchThrowableOfType(
                () -> adapter(3, true).fetchCurrentIntegration(), CatedraException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo("CATEDRA_AUTH_FAILED");
        assertThat(exception.getHttpStatus()).isEqualTo(503);
        assertThat(server.getRequestCount()).isOne();
    }

    @Test
    void retriesTransientServerErrors() {
        server.enqueue(json(500, "{\"status\":500}"));
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(200, integrationJson()));

        CatedraIntegration result = adapter(3, true).fetchCurrentIntegration();

        assertThat(result.getGroupId()).isEqualTo("proyecto-abc");
        assertThat(server.getRequestCount()).isEqualTo(3);
    }

    @Test
    void givesUpAfterTheConfiguredNumberOfAttempts() {
        server.enqueue(json(503, "{\"status\":503}"));
        server.enqueue(json(503, "{\"status\":503}"));

        CatedraException exception = catchThrowableOfType(
                () -> adapter(2, true).fetchCurrentIntegration(), CatedraException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo("CATEDRA_UNAVAILABLE");
        assertThat(exception.getHttpStatus()).isEqualTo(503);
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void reauthenticatesExactlyOnceWhenTheTokenIsRejected() throws Exception {
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(401, "{\"status\":401,\"detail\":\"Token expired\"}"));
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(200, integrationJson()));

        CatedraIntegration result = adapter(3, true).fetchCurrentIntegration();

        assertThat(result.getGroupId()).isEqualTo("proyecto-abc");
        assertThat(server.getRequestCount()).isEqualTo(4);
    }

    @Test
    void propagatesTheStatusCodeAndCodeOfTheCatedra() {
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(404,
                "{\"status\":404,\"title\":\"Not Found\",\"detail\":\"No integration\",\"code\":\"STUDENT_INTEGRATION_NOT_FOUND\"}"));

        CatedraException exception = catchThrowableOfType(
                () -> adapter(3, true).fetchCurrentIntegration(), CatedraException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo("STUDENT_INTEGRATION_NOT_FOUND");
        assertThat(exception.getHttpStatus()).isEqualTo(404);
        assertThat(exception.getDetail()).isEqualTo("No integration");
        // A 404 is not an authentication problem: no re-authentication is triggered.
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void failsFastWhenTheTechnicalCredentialsAreNotConfigured() {
        CatedraException exception = catchThrowableOfType(
                () -> adapterWithCredentials("", "").fetchCurrentIntegration(), CatedraException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo("CATEDRA_NOT_CONFIGURED");
        assertThat(exception.getHttpStatus()).isEqualTo(503);
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void rejectsAnIntegrationPayloadThatDoesNotMatchTheContract() {
        server.enqueue(json(200, authenticationJson()));
        server.enqueue(json(200, "{\"somethingElse\":true}"));

        CatedraException exception = catchThrowableOfType(
                () -> adapter(3, true).fetchCurrentIntegration(), CatedraException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo("CATEDRA_INVALID_RESPONSE");
        assertThat(exception.getHttpStatus()).isEqualTo(503);
    }

    @Test
    void answersUnavailableWhenTheCatedraIsNotReachable() {
        CatedraProperties properties = new CatedraProperties(
                new CatedraProperties.Api("http://127.0.0.1:1", "tech-login", "tech-password", ""),
                new CatedraProperties.Http(200, 200),
                new CatedraProperties.Retry(2, 1, 2),
                redis(),
                kafka());

        CatedraException exception = catchThrowableOfType(
                () -> adapter(properties).fetchCurrentIntegration(), CatedraException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getCode()).isEqualTo("CATEDRA_UNAVAILABLE");
        assertThat(exception.getHttpStatus()).isEqualTo(503);
    }

    private CatedraIntegrationGatewayAdapter adapter(int maxAttempts, boolean withCredentials) {
        CatedraProperties.Api api = new CatedraProperties.Api(
                server.url("/").toString(),
                withCredentials ? "tech-login" : "",
                withCredentials ? "tech-password" : "",
                "");
        CatedraProperties properties = new CatedraProperties(
                api,
                new CatedraProperties.Http(1000, 3000),
                new CatedraProperties.Retry(maxAttempts, 1, 2),
                redis(),
                kafka());
        return adapter(properties);
    }

    private CatedraIntegrationGatewayAdapter adapterWithCredentials(String login, String password) {
        CatedraProperties properties = new CatedraProperties(
                new CatedraProperties.Api(server.url("/").toString(), login, password, ""),
                new CatedraProperties.Http(1000, 3000),
                new CatedraProperties.Retry(1, 1, 2),
                redis(),
                kafka());
        return adapter(properties);
    }

    private CatedraIntegrationGatewayAdapter adapter(CatedraProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().build());
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        ObjectMapper objectMapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        CatedraTechnicalClient technicalClient = new CatedraTechnicalClient(
                properties,
                RestClient.builder().requestFactory(requestFactory).build(),
                objectMapper,
                new CatedraTokenHolder(),
                new RetryExecutor(properties));
        return new CatedraIntegrationGatewayAdapter(
                technicalClient,
                objectMapper,
                new CatedraIntegrationResponseMapper());
    }

    private static CatedraProperties.Redis redis() {
        return new CatedraProperties.Redis("redis.catedra.internal", 6379, "grp_proyecto-abc", "redis-secret",
                "catedra:sync:*", "alumnos:proyecto-abc:*");
    }

    private static CatedraProperties.Kafka kafka() {
        return new CatedraProperties.Kafka("kafka.catedra.internal:9092", "alumnos-proyecto-abc",
                "catedra.catalog.proyecto-abc", "alumnos.turnos.acciones.proyecto-abc",
                "catedra.turnos.telefono.proyecto-abc");
    }

    private static MockResponse json(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private static String authenticationJson() {
        return "{\"id_token\":\"technical-token-1\",\"integration\":" + integrationJson() + "}";
    }

    private static String integrationJson() {
        return """
                {
                  "groupId": "proyecto-abc",
                  "redisHost": "redis.catedra.internal",
                  "redisPort": 6379,
                  "redisUsername": "grp_proyecto-abc",
                  "redisPassword": "redis-secret",
                  "redisReadNamespace": "catedra:sync:*",
                  "redisWriteNamespace": "alumnos:proyecto-abc:*",
                  "kafkaBootstrapServers": "kafka.catedra.internal:9092",
                  "kafkaConsumerGroupId": "alumnos-proyecto-abc",
                  "kafkaCatalogTopic": "catedra.catalog.proyecto-abc",
                  "kafkaAppointmentActionsTopic": "alumnos.turnos.acciones.proyecto-abc",
                  "kafkaAppointmentPhoneTopic": "catedra.turnos.telefono.proyecto-abc",
                  "provisioningStatus": "PROVISIONED"
                }
                """;
    }
}
