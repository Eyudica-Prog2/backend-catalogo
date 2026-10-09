package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest;

import ar.edu.unlp.turnos.catalog.catedra.domain.model.CatedraIntegration;
import ar.edu.unlp.turnos.catalog.catedra.domain.ports.out.CatedraIntegrationGateway;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto.IntegrationResponse;
import ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.mapper.CatedraIntegrationResponseMapper;
import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.shared.technical.CatedraTechnicalClient;
import ar.edu.unlp.turnos.catalog.shared.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter for the technical integration of contract v1 (section 5).
 *
 * <p>Everything that belongs to the remote call (technical authentication, token reuse,
 * bounded retries, timeouts and the translation of the catedra errors) is owned by the
 * shared {@link CatedraTechnicalClient}; this adapter only validates and maps the payload
 * of {@code GET /api/student/integration} into the domain model of the slice.</p>
 *
 * <p>A payload that does not match the contract answers {@code 503 CATEDRA_INVALID_RESPONSE}
 * instead of aborting the startup: the service must keep running with the stored values.</p>
 */
@Component
@RequiredArgsConstructor
public class CatedraIntegrationGatewayAdapter implements CatedraIntegrationGateway {

    private static final String INTEGRATION_PATH = "/api/student/integration";

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;
    private final CatedraIntegrationResponseMapper responseMapper;

    @Override
    public CatedraIntegration fetchCurrentIntegration() {
        String responseBody = technicalClient.get(INTEGRATION_PATH);
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
}
