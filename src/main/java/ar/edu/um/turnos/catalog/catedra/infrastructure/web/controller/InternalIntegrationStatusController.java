package ar.edu.um.turnos.catalog.catedra.infrastructure.web.controller;

import ar.edu.um.turnos.catalog.catedra.domain.ports.in.GetIntegrationStatusUseCase;
import ar.edu.um.turnos.catalog.catedra.infrastructure.web.dto.IntegrationStatusResponse;
import ar.edu.um.turnos.catalog.catedra.infrastructure.web.mapper.IntegrationStatusDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the technical integration held by this service.
 *
 * <p>Protected by a valid end user JWT: in later iterations {@code backend-turnos} calls it
 * to check the health of the integration. The controller only maps DTOs and delegates to the
 * use case; it holds no business rule and no data access.</p>
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalIntegrationStatusController {

    private final GetIntegrationStatusUseCase getIntegrationStatus;
    private final IntegrationStatusDtoMapper mapper;

    @GetMapping("/integration-status")
    public ResponseEntity<IntegrationStatusResponse> getIntegrationStatus() {
        return ResponseEntity.ok(mapper.toResponse(getIntegrationStatus.getIntegrationStatus()));
    }
}
