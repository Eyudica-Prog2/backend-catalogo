package ar.edu.um.turnos.catalog.catalog.domain.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Version window published by the catedra in Redis (contract v1, section 14.2).
 *
 * <ul>
 *   <li>{@code currentVersion}: newest version available;</li>
 *   <li>{@code oldestAvailableVersion}: oldest version for which
 *       {@code catedra:sync:changes:{version}} is still published;</li>
 *   <li>{@code publishedAt}: instant of the publication, only used for diagnostics.</li>
 * </ul>
 *
 * <p>The window is the contract that decides between an incremental application and a full
 * snapshot: a local version older than {@code oldestAvailableVersion} cannot be continued
 * safely (section 18.2 of the reference).</p>
 */
@Getter
@Builder
public class CatalogVersionWindow {

    private final long currentVersion;
    private final long oldestAvailableVersion;
    private final String publishedAt;

    /**
     * Checks that the published window can be used at all.
     *
     * @throws IllegalArgumentException with a self contained message when it cannot
     */
    public void validate() {
        if (currentVersion < 0) {
            throw new IllegalArgumentException(
                    "Redis publishes a negative currentVersion: " + currentVersion + ".");
        }
        if (oldestAvailableVersion < 0) {
            throw new IllegalArgumentException(
                    "Redis publishes a negative oldestAvailableVersion: " + oldestAvailableVersion + ".");
        }
        if (oldestAvailableVersion > currentVersion) {
            throw new IllegalArgumentException(
                    "Redis publishes oldestAvailableVersion=" + oldestAvailableVersion
                            + " above currentVersion=" + currentVersion + ".");
        }
    }

    /**
     * @param appliedVersion version currently stored by this service
     * @return true when the local copy can be continued incrementally towards
     *         {@code currentVersion} without losing the link between versions
     */
    public boolean canBeFollowedFrom(long appliedVersion) {
        return appliedVersion >= oldestAvailableVersion && appliedVersion <= currentVersion;
    }
}
