package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest;

/**
 * Marks a call to the catedra that failed in a way worth retrying (HTTP 5xx or timeout).
 *
 * <p>When the bounded retry policy is exhausted the adapter translates this exception into
 * {@code CATEDRA_UNAVAILABLE}; it never reaches the controllers.</p>
 */
class RetryableCatedraCallException extends RuntimeException {

    private final int statusCode;

    RetryableCatedraCallException(String operation, int statusCode) {
        super("Retryable failure while calling " + operation + " (HTTP " + statusCode + ").");
        this.statusCode = statusCode;
    }

    /**
     * @return HTTP status of the failed attempt, 0 when the call never completed
     */
    int getStatusCode() {
        return statusCode;
    }
}
