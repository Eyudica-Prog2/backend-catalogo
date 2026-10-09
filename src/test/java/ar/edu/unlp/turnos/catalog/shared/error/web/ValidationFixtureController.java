package ar.edu.unlp.turnos.catalog.shared.error.web;

import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test only controller. It exists to exercise the global exception handler end to end,
 * because the production endpoints of this iteration do not accept a request body.
 */
@RestController
@RequestMapping("/test/validation")
public class ValidationFixtureController {

    @PostMapping("/echo")
    public ResponseEntity<EchoResponse> echo(@Valid @RequestBody EchoRequest request) {
        return ResponseEntity.ok(new EchoResponse(request.name()));
    }

    @PostMapping("/catedra-auth-failed")
    public void catedraAuthFailed() {
        throw CatedraException.authFailed("The catedra rejected the technical credentials of this project.");
    }

    @PostMapping("/catedra-not-found")
    public void catedraNotFound() {
        throw CatedraException.integrationNotFound();
    }

    public record EchoRequest(@NotBlank String name) {
    }

    public record EchoResponse(String name) {
    }
}
