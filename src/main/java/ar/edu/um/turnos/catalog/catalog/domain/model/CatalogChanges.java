package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Content of {@code catedra:sync:changes:{version}} (contract v1, section 14.5).
 *
 * <p>It holds the ids affected by one version, <strong>not</strong> the records
 * themselves: to apply the version the current state of every id is read from the
 * corresponding hash of Redis. A record with {@code enabled=false} is a logical deletion
 * and must be applied as such, never removed from the local copy.</p>
 *
 * <p>An empty set of ids is valid: it means that the version changed nothing that this
 * service stores, and the version must still be advanced.</p>
 */
@Getter
@Builder
public class CatalogChanges {

    private final long version;
    private final List<Long> professionalCategoryIds;
    private final List<Long> professionalIds;
    private final List<Long> weeklyScheduleIds;

    /**
     * All-args constructor used by the builder. Normalizes absent collections to an empty
     * list, so callers never have to defend against {@code null}.
     *
     * @param version                 version these changes belong to
     * @param professionalCategoryIds affected category ids, may be null
     * @param professionalIds         affected professional ids, may be null
     * @param weeklyScheduleIds       affected weekly schedule ids, may be null
     */
    public CatalogChanges(long version, List<Long> professionalCategoryIds, List<Long> professionalIds,
                          List<Long> weeklyScheduleIds) {
        this.version = version;
        this.professionalCategoryIds = normalize(professionalCategoryIds);
        this.professionalIds = normalize(professionalIds);
        this.weeklyScheduleIds = normalize(weeklyScheduleIds);
    }

    /**
     * @return true when the version does not affect anything this service stores
     */
    public boolean isEmpty() {
        return professionalCategoryIds.isEmpty() && professionalIds.isEmpty()
                && weeklyScheduleIds.isEmpty();
    }

    /**
     * Checks that the published delta belongs to the version that was requested, so a stale
     * or misrouted payload can never advance the local version.
     *
     * @param requestedVersion version this service asked for
     * @throws IllegalArgumentException with a self contained message when it does not match
     */
    public void validateFor(long requestedVersion) {
        if (version != requestedVersion) {
            throw new IllegalArgumentException(
                    "Redis answered changes for version " + version
                            + " when version " + requestedVersion + " was requested.");
        }
    }

    private static List<Long> normalize(List<Long> ids) {
        return ids == null ? List.of() : List.copyOf(ids);
    }
}
