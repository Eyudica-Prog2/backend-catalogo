package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * State of the catalog synchronization, stored as a single row.
 *
 * <ul>
 *   <li>{@code snapshotVersion}: last version received from the catedra (attempted);</li>
 *   <li>{@code appliedVersion}: version of the data currently stored in the local copy. It
 *       only advances when the three collections were applied in the same transaction;</li>
 *   <li>{@code lastError}/{@code lastErrorAt}: last failed attempt, cleared on success;</li>
 *   <li>{@code lastSyncResult}: how the last attempt ended (snapshot, incremental, nothing
 *       to do or failure);</li>
 *   <li>{@code currentVersion}/{@code oldestAvailableVersion}: window observed in Redis
 *       during the last attempt. The status endpoint refreshes them from Redis and falls
 *       back to these stored values when Redis is unreachable.</li>
 * </ul>
 *
 * <p>{@code snapshotVersion > appliedVersion} together with a {@code lastError} means: the
 * catedra has a newer version that this service could not apply yet.</p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class CatalogSyncState {

    private long snapshotVersion;
    private long appliedVersion;
    private Instant lastSyncAt;
    private String lastError;
    private Instant lastErrorAt;
    private SyncResult lastSyncResult;
    private Long currentVersion;
    private Long oldestAvailableVersion;

    /**
     * State reported before the first successful synchronization.
     *
     * @return a state with version zero and no error
     */
    public static CatalogSyncState notSynced() {
        return CatalogSyncState.builder().snapshotVersion(0L).appliedVersion(0L).build();
    }

    /**
     * @return true when a snapshot was applied at least once
     */
    public boolean isSynced() {
        return appliedVersion > 0;
    }

    /**
     * @return true when the last attempt failed, i.e. there is something to report
     */
    public boolean hasError() {
        return Objects.nonNull(lastError) && !lastError.isBlank();
    }
}
