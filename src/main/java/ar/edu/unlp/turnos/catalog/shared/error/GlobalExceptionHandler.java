package ar.edu.unlp.turnos.catalog.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The single place where exceptions become {@code application/problem+json} responses.
 *
 * <p>No controller performs a manual {@code try/catch} to translate errors into HTTP status
 * codes: this advice owns that translation, so the functional {@code code} is never lost and
 * no endpoint repeats the same block.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception,
                                                          HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getObjectName(), error.getField(), error.getDefaultMessage()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "Request validation failed.", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception,
                                                              HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "The request body is missing or malformed.", request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException exception,
                                                                HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "A required request parameter is missing.", request, List.of());
    }

    /**
     * A parameter that cannot be converted into the expected type: {@code page=abc},
     * {@code categoryId=abc} or {@code date=not-a-date}. Without this handler the
     * {@code IllegalArgumentException} would reach the catch-all and be reported as an
     * internal error instead of a client mistake.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
                                                            HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
                "A request parameter does not have the expected format.", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
                                                                HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.METHOD_NOT_ALLOWED,
                "The HTTP method is not supported for this endpoint.", request, List.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
                                                                    HttpServletRequest request) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                "The content type of the request is not supported.", request, List.of());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ProblemDetail> handleUnknownEndpoint(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, ErrorCodes.ENDPOINT_NOT_FOUND,
                "The requested endpoint does not exist.", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception,
                                                            HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
                "The authenticated identity is not allowed to perform this operation.", request, List.of());
    }

    /**
     * Errors produced by this service (including the ones that come from the catedra) are
     * propagated with their own status and code, keeping this service aligned with the
     * reference contract.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        log.warn("API error response: code={}, status={}", exception.getCode(), exception.getHttpStatus());
        return problem(HttpStatus.valueOf(exception.getHttpStatus()), exception.getCode(), exception.getDetail(),
                request, exception.getFieldErrors());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception while processing {} {}", request.getMethod(), request.getRequestURI(),
                exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR,
                "An unexpected error occurred. See the service logs for details.", request, List.of());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail,
                                                  HttpServletRequest request, List<FieldErrorDetail> fieldErrors) {
        ProblemDetail body = ProblemResponses.create(status, code, detail, request.getRequestURI(), fieldErrors);
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
