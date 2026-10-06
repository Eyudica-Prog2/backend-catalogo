package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest;

import ar.edu.unlp.turnos.catalog.catedra.application.exception.CatedraException;
import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.out.CatedraIntegrationGateway;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto.AuthenticationRequest;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto.AuthenticationResponse;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto.IntegrationResponse;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.mapper.CatedraIntegrationResponseMapper;
import ar.edu.unlp.turnos.catalog.shared.config.CatedraProperties;
import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.shared.error.FieldErrorDetail;
import ar.edu.unlp.turnos.catalog.shared.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Outbound adapter for the technical integration of contract v1 (section 5).
 *
 * <p>It owns everything that belongs to the remote call: technical authentication,
 * token reuse, bounded retries with backoff, response validation and the translation of
 * the catedra errors into {@link CatedraException}.</p>
 *
 * <p>Behaviours worth documenting:</p>
 * <ul>
 *   <li>a rejected technical login answers {@code 503 CATEDRA_AUTH_FAILED} instead of
 *       {@code 401}, because a {@code 401} would blame the caller's own token;</li>
 *   <li>a rejected token on the integration endpoint triggers exactly one re-authentication
 *       and one retry, never a loop;</li>
 *   <li>the technical token is held in memory only and is never logged.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CatedraIntegrationGatewayAdapter implements CatedraIntegrationGateway {

    private static final Logger log = LoggerFactory.getLogger(CatedraIntegrationGatewayAdapter.class);

    private static final String AUTHENTICATE_PATH = "/api/authenticate";
    private static final String INTEGRATION_PATH = "/api/student/integration";
    private static final String BEARER_PREFIX = "Bearer ";

    private static final Predicate<RuntimeException> RETRYABLE =
            failure -> failure instanceof ResourceAccessException || failure instanceof RetryableCatedraCallException;

    private final CatedraProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final CatedraTokenHolder tokenHolder;
    private final RetryExecutor retryExecutor;
    private final CatedraIntegrationResponseMapper responseMapper;

    @Override
    public CatedraIntegration fetchCurrentIntegration() {
        String token = tokenHolder.currentToken().orElseGet(this::authenticate);
        try {
            return fetchIntegration(token);
        } catch (RejectedTokenException firstRejection) {
            log.info("The catedra rejected the technical token; requesting a new one.");
            tokenHolder.clear();
            try {
                return fetchIntegration(authenticate());
            } catch (RejectedTokenException secondRejection) {
                throw CatedraException.authFailed(
                        "The catedra keeps rejecting the technical token of this project.");
            }
        }
    }

    /**
     * Obtains a technical token: uses {@code CATEDRA_JWT} when present, otherwise performs
     * {@code POST /api/authenticate} with {@code rememberMe=true}.
     */
    private String authenticate() {
        CatedraProperties.Api api = properties.api();
        if (Strings.isNotBlank(api.jwt())) {
            return api.jwt();
        }
        if (!api.hasCredentials()) {
            throw CatedraException.notConfigured(
                    "The technical credentials of this project are not configured.");
        }
        return requestTechnicalToken();
    }

    private String requestTechnicalToken() {
        AuthenticationRequest body = new AuthenticationRequest(
                properties.api().login(), properties.api().password(), true);

        String responseBody = withRetry("POST " + AUTHENTICATE_PATH,
                () -> post(AUTHENTICATE_PATH, body));

        AuthenticationResponse response;
        try {
            response = objectMapper.readValue(responseBody, AuthenticationResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw CatedraException.invalidResponse(
                    "The catedra authentication response could not be parsed.", ex);
        }
        if (response == null || Strings.isBlank(response.idToken()) || response.integration() == null) {
            throw CatedraException.invalidResponse(
                    "The catedra authentication response is missing id_token or integration.");
        }
        tokenHolder.store(response.idToken());
        return response.idToken();
    }

    private CatedraIntegration fetchIntegration(String token) {
        String responseBody = withRetry("GET " + INTEGRATION_PATH, () -> get(INTEGRATION_PATH, token));
        if (Strings.isBlank(responseBody)) {
            throw CatedraException.invalidResponse("The catedra integration response body is empty.");
        }

        IntegrationResponse response;
        try {
            response = objectMapper.readValue(responseBody, IntegrationResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw CatedraException.invalidResponse(
                    "The catedra integration response could not be parsed.", ex);
        }
        if (!responseMapper.isUsable(response)) {
            throw CatedraException.invalidResponse(
                    "The catedra integration response does not contain a usable integration.");
        }
        try {
            return responseMapper.toDomain(response);
        } catch (IllegalArgumentException ex) {
            throw CatedraException.invalidResponse(
                    "The catedra integration response does not match the contract.", ex);
        }
    }

    private String post(String path, Object body) {
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(absoluteUri(path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
            return response.getBody();
        } catch (RestClientResponseException exception) {
            throw failure("POST " + path, exception, true);
        }
    }

    private String get(String path, String token) {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(absoluteUri(path))
                    .header(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)
                    .retrieve()
                    .toEntity(String.class);
            return response.getBody();
        } catch (RestClientResponseException exception) {
            throw failure("GET " + path, exception, false);
        }
    }

    /**
     * Runs an action applying the bounded retry policy and translating the exhausted or
     * unreachable cases into the public {@code CATEDRA_UNAVAILABLE} error.
     */
    private <T> T withRetry(String operation, Supplier<T> action) {
        try {
            return retryExecutor.execute(operation, action, RETRYABLE);
        } catch (RetryableCatedraCallException exhausted) {
            log.warn("Call {} failed after {} attempts: last status {}",
                    operation, Math.max(1, properties.retry().maxAttempts()), exhausted.getStatusCode());
            throw CatedraException.unavailable("The catedra API is not available.");
        } catch (ResourceAccessException unreachable) {
            log.warn("Call {} could not reach the catedra: {}", operation, unreachable.getMessage());
            throw CatedraException.unavailable("The catedra API is not reachable.");
        }
    }

    /**
     * Classifies an error answer of the catedra.
     *
     * @param operation        label of the failing call
     * @param exception        exception raised by the HTTP client with status and body
     * @param authenticationCall true when the failing call was {@code /api/authenticate}
     * @return the exception that must be thrown by the caller
     */
    private RuntimeException failure(String operation, RestClientResponseException exception,
                                     boolean authenticationCall) {
        HttpStatusCode status = exception.getStatusCode();
        int statusCode = status.value();
        ProblemPayload payload = parseProblemBody(exception.getResponseBodyAsString());

        if (authenticationCall && (statusCode == 401 || statusCode == 403)) {
            return CatedraException.authFailed(
                    "The catedra rejected the technical credentials of this project.");
        }
        if (!authenticationCall && statusCode == 401) {
            return new RejectedTokenException();
        }
        if (status.is5xxServerError()) {
            return new RetryableCatedraCallException(operation, statusCode);
        }
        if (payload != null) {
            return CatedraException.upstream(statusCode, payload.code(), payload.detail(), payload.fieldErrors());
        }
        return CatedraException.upstream(statusCode, ErrorCodes.CATEDRA_ERROR,
                "The catedra rejected the request with HTTP " + statusCode + ".", List.of());
    }

    private ProblemPayload parseProblemBody(String body) {
        if (Strings.isBlank(body)) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node == null || !node.isObject()) {
                return null;
            }
            String code = textOrNull(node, "code");
            String detail = textOrNull(node, "detail");
            List<FieldErrorDetail> fieldErrors = readFieldErrors(node.get("fieldErrors"));
            if (code == null && detail == null && fieldErrors.isEmpty()) {
                return null;
            }
            return new ProblemPayload(
                    code != null ? code : ErrorCodes.CATEDRA_ERROR,
                    detail != null ? detail : "The catedra API reported an error.",
                    fieldErrors);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private List<FieldErrorDetail> readFieldErrors(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<FieldErrorDetail> fieldErrors = new ArrayList<>();
        for (JsonNode entry : node) {
            if (entry != null && entry.isObject()) {
                fieldErrors.add(new FieldErrorDetail(
                        textOrNull(entry, "objectName"),
                        textOrNull(entry, "field"),
                        textOrNull(entry, "message")));
            }
        }
        return fieldErrors;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String absoluteUri(String path) {
        return properties.api().baseUrl().replaceAll("/+$", "") + path;
    }

    private record ProblemPayload(String code, String detail, List<FieldErrorDetail> fieldErrors) {
    }
}
