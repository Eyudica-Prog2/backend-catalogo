package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest;

/**
 * Signals that the technical token sent to the catedra was rejected (HTTP 401) and that a
 * fresh token must be requested once.
 *
 * <p>Infrastructure only: it never crosses the boundary of the slice.</p>
 */
class RejectedTokenException extends RuntimeException {

    RejectedTokenException() {
        super("The technical token was rejected by the catedra.");
    }
}
