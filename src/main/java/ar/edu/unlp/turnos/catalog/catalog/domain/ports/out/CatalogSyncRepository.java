package ar.edu.unlp.turnos.catalog.catalog.domain.ports.out;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.SyncResult;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Outbound port: write access to the local copy and to the synchronization state.
 *
 * <p>The writes that matter for the consistency of the service are {@link #applySnapshot}
 * (the three collections and the applied version must move together),
 * {@link #applyIncremental} (one version and the applied version must move together) and
 * {@link #recordFailure} (records an attempt that must not advance the applied version).</p>
 */
public interface CatalogSyncRepository {

    /**
     * @return the stored state, empty when this service never synchronized
     */
    Optional<CatalogSyncState> findState();

    /**
     * Replaces the three collections and stores the version of the snapshot in a single
     * transaction: either everything is applied or nothing is.
     *
     * @param snapshot validated snapshot
     * @return the state after the snapshot was applied
     */
    CatalogSyncState applySnapshot(CatalogSnapshot snapshot);

    /**
     * Records a failed attempt without touching the local copy nor the applied version.
     *
     * <p>The attempt is also marked as {@link SyncResult#FAILED}, so the status tells
     * <em>how</em> the last synchronization ended and not only what it left behind.</p>
     *
     * @param attemptedSnapshotVersion version that failed, {@code null} when the download
     *                                 itself failed and no version was received
     * @param error                   self contained description of the failure
     * @param failedAt                instant of the failure
     */
    void recordFailure(Long attemptedSnapshotVersion, String error, Instant failedAt);

    /**
     * Applies the changes of a single incremental version and advances the applied version
     * in one transaction: either the whole version is stored or nothing is, and the version
     * only moves when its changes are already written.
     *
     * <p>It is idempotent: applying the same version twice writes the same rows and leaves
     * the applied version unchanged (the ids come from the remote copy, there is no local
     * sequence involved).</p>
     *
     * @param version      version being applied
     * @param categories   current state of the affected categories
     * @param professionals current state of the affected professionals
     * @param schedules    current state of the affected weekly schedules
     * @return the state after the version was applied
     */
    CatalogSyncState applyIncremental(long version, List<ProfessionalCategory> categories,
                                      List<Professional> professionals,
                                      List<WeeklySchedule> schedules);

    /**
     * Stores the version window observed in Redis. It is an observation used as a fallback
     * by {@code GET /api/sync/status} when Redis is unreachable; it never decides anything.
     *
     * @param window observed window
     */
    void recordVersionWindow(CatalogVersionWindow window);

    /**
     * Records that the last synchronization ended successfully, clearing any previous
     * failure.
     *
     * @param result    outcome to store
     * @param succeededAt instant the attempt finished
     */
    void recordSuccess(SyncResult result, Instant succeededAt);
}
