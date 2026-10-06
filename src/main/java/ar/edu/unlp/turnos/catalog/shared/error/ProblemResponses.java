package ar.edu.unlp.turnos.catalog.shared.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Builds the {@code application/problem+json} bodies used by the whole service.
 *
 * <p>Shape follows the reference contract of the catedra (section 13): {@code type},
 * {@code title}, {@code status}, {@code detail}, {@code message}, {@code path}, {@code code}
 * and, for validation failures, {@code fieldErrors}.</p>
 *
 * <p>It is a plain utility (no Spring beans) so the single {@code @RestControllerAdvice}
 * and the security entry point can share it without extra wiring.</p>
 */
public final class ProblemResponses {

    public static final String VALIDATION_TYPE =
            "https://www.jhipster.tech/problem/constraint-violation";
    public static final String GENERIC_TYPE =
            "https://www.jhipster.tech/problem/problem-with-message";

    private ProblemResponses() {
    }

    /**
     * Creates the problem body.
     *
     * @param status      HTTP status
     * @param code        stable functional code from {@link ErrorCodes} or from the catedra
     * @param detail      self contained explanation, never built by string concatenation
     * @param path        request URI that produced the error
     * @param fieldErrors failing fields, ignored when null or empty
     * @return the problem detail ready to be serialized
     */
    public static ProblemDetail create(HttpStatus status, String code, String detail, String path,
                                       List<FieldErrorDetail> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create(ErrorCodes.VALIDATION_ERROR.equals(code) ? VALIDATION_TYPE : GENERIC_TYPE));
        problem.setProperty("message", ErrorCodes.VALIDATION_ERROR.equals(code)
                ? "error.validation"
                : "error.http." + status.value());
        problem.setProperty("path", path);
        problem.setProperty("code", code);
        // RFC 7807 "instance": Spring MVC fills it automatically for ProblemDetail bodies
        // returned from a controller, but the security entry point writes the body directly,
        // so it is set here to keep every error response uniform.
        if (path != null && !path.isBlank()) {
            try {
                problem.setInstance(URI.create(path));
            } catch (IllegalArgumentException invalidUri) {
                // Not a valid URI reference: keep the response without "instance".
            }
        }
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return problem;
    }

    /**
     * Writes a problem body straight to the response. Used by the security layer, which runs
     * before the controller advice.
     *
     * @param response     response to write to
     * @param objectMapper application wide mapper
     * @param status       HTTP status
     * @param code         stable functional code
     * @param detail       self contained explanation
     * @param path         request URI
     * @throws IOException when the response stream cannot be written
     */
    public static void write(HttpServletResponse response, ObjectMapper objectMapper, HttpStatus status,
                             String code, String detail, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(),
                create(status, code, detail, path, List.of()));
    }
}
