package ar.edu.unlp.turnos.catalog.shared.security;

import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.shared.error.ProblemResponses;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Answers {@code 403} as {@code application/problem+json} with a stable {@code code}.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        ProblemResponses.write(response, objectMapper, HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                "The authenticated identity is not allowed to perform this operation.",
                request.getRequestURI());
    }
}
