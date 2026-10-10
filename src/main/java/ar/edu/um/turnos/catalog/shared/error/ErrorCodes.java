package ar.edu.um.turnos.catalog.shared.error;

/**
 * Stable functional error codes returned in the {@code code} property of every
 * {@code application/problem+json} response.
 *
 * <p>Clients must branch on {@code status} + {@code code} and never on {@code detail}
 * (contract v1, section 13).</p>
 */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    // Generic errors produced by this service.
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String ENDPOINT_NOT_FOUND = "ENDPOINT_NOT_FOUND";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // Integration with the catedra service.
    public static final String CATEDRA_AUTH_FAILED = "CATEDRA_AUTH_FAILED";
    public static final String CATEDRA_UNAVAILABLE = "CATEDRA_UNAVAILABLE";
    public static final String CATEDRA_NOT_CONFIGURED = "CATEDRA_NOT_CONFIGURED";
    public static final String CATEDRA_INVALID_RESPONSE = "CATEDRA_INVALID_RESPONSE";
    public static final String CATEDRA_INTEGRATION_NOT_FOUND = "CATEDRA_INTEGRATION_NOT_FOUND";
    public static final String CATEDRA_ERROR = "CATEDRA_ERROR";

    // Local copy of the catalog (snapshot, search and internal contract).
    public static final String PROFESSIONAL_NOT_FOUND = "PROFESSIONAL_NOT_FOUND";
    public static final String SNAPSHOT_INVALID = "SNAPSHOT_INVALID";

    // End user registration and authentication (compatible with the JHipster contract).
    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String EMAIL_ALREADY_EXISTS = "EMAIL_ALREADY_EXISTS";
}
