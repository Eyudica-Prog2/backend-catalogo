package ar.edu.unlp.turnos.catalog.shared.web.controller;

import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness style endpoint. It is the only public endpoint of the service and is used by the
 * docker compose health check and by the operator after {@code docker compose up}.
 */
@RestController
@RequestMapping("/api/internal")
public class InternalHealthController {

    public static final String STATUS_UP = "UP";

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse(STATUS_UP, "backend-catalogo", Instant.now()));
    }

    /**
     * @param status    always {@code UP} while the HTTP layer answers
     * @param service   logical name of this service
     * @param timestamp instant of the answer
     */
    public record HealthResponse(String status, String service, Instant timestamp) {
    }
}
