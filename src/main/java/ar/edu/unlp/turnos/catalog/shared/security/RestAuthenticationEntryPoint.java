package ar.edu.unlp.turnos.catalog.shared.security;

import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.shared.error.ProblemResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Answers {@code 401} as {@code application/problem+json} with a stable {@code code},
 * instead of the empty body (or HTML error page) that Spring Security returns by default.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        ProblemResponses.write(response, objectMapper, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED,
                "A valid authentication token is required to access this resource.",
                request.getRequestURI());
    }
}
