package ar.edu.unlp.turnos.catalog.shared.error;

import java.util.List;

/**
 * Error produced while calling the catedra API on behalf of any slice.
 *
 * <p>It lives in {@code shared} (instead of a slice) because the technical call machinery of
 * {@code shared/technical} is used by two capabilities: the technical integration
 * ({@code catedra} slice) and the catalog snapshot download ({@code catalog} slice). Its
 * functional codes were already declared in the shared {@link ErrorCodes}.</p>
 *
 * <p>Every instance carries a stable functional {@code code}, the HTTP status that must be
 * exposed and a self contained {@code detail}, so the shared error handler can translate it
 * without rebuilding a message at the catching point.</p>
 *
 * <p>Decision documented in the README: failures caused by our own technical credentials or
 * by an unreachable catedra are answered with {@code 503} instead of {@code 401}, because a
 * {@code 401} would make the caller believe that <em>its</em> token is invalid.</p>
 */
public class CatedraException extends ApiException {

    private CatedraException(String code, int httpStatus, String detail,
                             List<FieldErrorDetail> fieldErrors, Throwable cause) {
        super(code, httpStatus, detail, fieldErrors, cause);
    }

    /**
     * The catedra rejected the technical credentials of this project.
     */
    public static CatedraException authFailed(String detail) {
        return new CatedraException(ErrorCodes.CATEDRA_AUTH_FAILED, 503, detail, List.of(), null);
    }

    /**
     * The catedra is unreachable, timed out or answered with a server error.
     */
    public static CatedraException unavailable(String detail) {
        return new CatedraException(ErrorCodes.CATEDRA_UNAVAILABLE, 503, detail, List.of(), null);
    }

    /**
     * The service was started without technical credentials.
     */
    public static CatedraException notConfigured(String detail) {
        return new CatedraException(ErrorCodes.CATEDRA_NOT_CONFIGURED, 503, detail, List.of(), null);
    }

    /**
     * The catedra answered with a payload that does not match the contract.
     */
    public static CatedraException invalidResponse(String detail) {
        return invalidResponse(detail, null);
    }

    /**
     * The catedra answered with a payload that does not match the contract.
     */
    public static CatedraException invalidResponse(String detail, Throwable cause) {
        return new CatedraException(ErrorCodes.CATEDRA_INVALID_RESPONSE, 503, detail, List.of(), cause);
    }

    /**
     * The technical integration is not stored locally (yet).
     */
    public static CatedraException integrationNotFound() {
        return new CatedraException(ErrorCodes.CATEDRA_INTEGRATION_NOT_FOUND, 404,
                "No technical integration has been stored by this service yet.", List.of(), null);
    }

    /**
     * Any other error returned by the catedra: its own status and code are propagated as is.
     *
     * @param httpStatus  status returned by the catedra
     * @param code        functional code returned by the catedra
     * @param detail      detail returned by the catedra
     * @param fieldErrors field errors returned by the catedra, may be empty
     */
    public static CatedraException upstream(int httpStatus, String code, String detail,
                                            List<FieldErrorDetail> fieldErrors) {
        return new CatedraException(code, httpStatus, detail, fieldErrors, null);
    }
}
