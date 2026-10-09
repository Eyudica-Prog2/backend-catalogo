package ar.edu.unlp.turnos.catalog.shared.error;

import java.util.List;

/**
 * Base exception for every error that must be exposed as {@code application/problem+json}
 * with a stable functional {@code code}.
 *
 * <p>Shared and slice specific exceptions (e.g. {@link CatedraException},
 * {@code CatalogException}) extend this class, so the shared error handler never has to
 * import a slice.</p>
 */
public class ApiException extends RuntimeException {

    private final String code;
    private final int httpStatus;
    private final String detail;
    private final List<FieldErrorDetail> fieldErrors;

    public ApiException(String code, int httpStatus, String detail) {
        this(code, httpStatus, detail, List.of(), null);
    }

    public ApiException(String code, int httpStatus, String detail, Throwable cause) {
        this(code, httpStatus, detail, List.of(), cause);
    }

    public ApiException(String code, int httpStatus, String detail, List<FieldErrorDetail> fieldErrors,
                        Throwable cause) {
        super(detail, cause);
        this.code = code;
        this.httpStatus = httpStatus;
        this.detail = detail;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getDetail() {
        return detail;
    }

    public List<FieldErrorDetail> getFieldErrors() {
        return fieldErrors;
    }
}
