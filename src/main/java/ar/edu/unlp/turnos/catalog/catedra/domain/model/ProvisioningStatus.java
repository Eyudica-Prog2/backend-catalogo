package ar.edu.unlp.turnos.catalog.catedra.domain.model;

/**
 * Provisioning state of the technical account, as reported by the catedra.
 *
 * <p>{@link #UNKNOWN} keeps the model tolerant to values added in later contract versions
 * instead of failing the whole integration refresh.</p>
 */
public enum ProvisioningStatus {
    PENDING,
    PROVISIONED,
    FAILED,
    REVOKED,
    UNKNOWN;

    /**
     * @param value raw value received from the catedra, may be null or blank
     * @return the matching constant, or {@link #UNKNOWN} when the value is not recognised
     */
    public static ProvisioningStatus from(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim();
        for (ProvisioningStatus status : values()) {
            if (status != UNKNOWN && status.name().equalsIgnoreCase(normalized)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
