package ar.edu.um.turnos.catalog.catalog.infrastructure.rest;

import ar.edu.um.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogSnapshotGateway;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.mapper.SnapshotResponseMapper;
import ar.edu.um.turnos.catalog.shared.technical.CatedraTechnicalClient;
import ar.edu.um.turnos.catalog.shared.util.Strings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Outbound adapter for {@code GET /api/synchronization/snapshot} (contract v1, section 7).
 *
 * <p>Everything that belongs to the remote call (technical authentication, token reuse,
 * bounded retries and timeouts) is owned by the shared {@link CatedraTechnicalClient}. This
 * adapter only reads the payload and translates it into the domain:</p>
 * <ul>
 *   <li>transport or authentication failures keep their {@code CATEDRA_*} code;</li>
 *   <li>a payload that cannot be read becomes {@code 503 SNAPSHOT_INVALID}, because what
 *       arrived is not a snapshot as the contract defines it.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CatalogSnapshotGatewayAdapter implements CatalogSnapshotGateway {

    private static final String SNAPSHOT_PATH = "/api/synchronization/snapshot";

    private final CatedraTechnicalClient technicalClient;
    private final ObjectMapper objectMapper;
    private final SnapshotResponseMapper responseMapper;

    @Override
    public CatalogSnapshot fetchCatalogSnapshot() {
        String responseBody = technicalClient.get(SNAPSHOT_PATH);
        if (Strings.isBlank(responseBody)) {
            throw CatalogException.invalidSnapshot("The snapshot published by the catedra is empty.");
        }

        SnapshotResponse response;
        try {
            response = objectMapper.readValue(responseBody, SnapshotResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            throw CatalogException.invalidSnapshot(
                    "The snapshot published by the catedra could not be parsed.");
        }
        try {
            return responseMapper.toDomain(response);
        } catch (IllegalArgumentException rejected) {
            throw CatalogException.invalidSnapshot(rejected.getMessage());
        }
    }
}
