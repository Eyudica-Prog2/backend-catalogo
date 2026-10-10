package ar.edu.um.turnos.catalog.catalog.application.exception;

import ar.edu.um.turnos.catalog.shared.error.ApiException;
import ar.edu.um.turnos.catalog.shared.error.ErrorCodes;

/**
 * Single application exception of the {@code catalog} slice.
 *
 * <p>Every instance carries a stable functional {@code code}, the HTTP status that must be
 * exposed and a self contained {@code detail}, so the shared error handler can translate it
 * without rebuilding a message at the catching point.</p>
 */
public class CatalogException extends ApiException {

    private CatalogException(String code, int httpStatus, String detail) {
        super(code, httpStatus, detail);
    }

    /**
     * The professional is not part of the local copy of the catalog.
     *
     * <p>Used by the public API and by the internal contract consumed by
     * {@code backend-turnos}, which expects {@code 404 PROFESSIONAL_NOT_FOUND}.</p>
     */
    public static CatalogException professionalNotFound() {
        return new CatalogException(ErrorCodes.PROFESSIONAL_NOT_FOUND, 404,
                "The requested professional does not exist in the local copy of the catalog.");
    }

    /**
     * The snapshot published by the catedra cannot be applied to the local copy.
     *
     * <p>Answered with {@code 503} and not with {@code 400}: the request that triggered the
     * synchronization is well formed, the payload that arrived from the catedra is not.</p>
     *
     * @param detail self contained description produced by the rule that rejected it
     */
    public static CatalogException invalidSnapshot(String detail) {
        return new CatalogException(ErrorCodes.SNAPSHOT_INVALID, 503, detail);
    }

    /**
     * A request parameter of the catalog API is not usable (page, size or sort).
     *
     * @param detail self contained description produced by the rule that rejected it
     */
    public static CatalogException invalidRequest(String detail) {
        return new CatalogException(ErrorCodes.VALIDATION_ERROR, 400, detail);
    }
}
