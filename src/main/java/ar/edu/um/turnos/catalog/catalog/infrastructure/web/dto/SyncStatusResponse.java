package ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto;

import java.time.Instant;

/**
 * Body of {@code GET /api/sync/status} and of the on demand synchronizations
 * ({@code POST /api/internal/sync/force} and {@code POST /api/internal/sync/run}).
 *
 * <p>Reported fields (iteration 03):</p>
 *
 * <ul>
 *   <li>{@code appliedVersion}: version of the data currently stored locally;</li>
 *   <li>{@code currentVersion} / {@code oldestAvailableVersion}: window published by the
 *       catedra in Redis, refreshed on every read and falling back to the values observed
 *       during the last attempt when the cache is unreachable;</li>
 *   <li>{@code lastSyncResult}: how the last attempt ended ({@code SNAPSHOT_APPLIED},
 *       {@code INCREMENTAL_APPLIED}, {@code ALREADY_CURRENT} or {@code FAILED});</li>
 *   <li>{@code lastError}: self contained description of the last failed attempt.</li>
 * </ul>
 *
 * <p>{@code snapshotVersion > appliedVersion} together with {@code lastError} means that the
 * catedra has a newer version that could not be applied yet; the local copy keeps serving the
 * data of {@code appliedVersion} meanwhile. Fields without value are omitted from the JSON
 * (the service serializes with {@code non_null}).</p>
 *
 * @param appliedVersion         version of the data currently stored
 * @param snapshotVersion        last version received from the catedra
 * @param currentVersion         newest version published in Redis, null when never observed
 * @param oldestAvailableVersion oldest version whose changes are still published, null when never observed
 * @param lastSyncResult         outcome of the last attempt, null before the first attempt
 * @param lastSyncAt             instant of the last successful application
 * @param lastError              self contained description of the last failed attempt
 * @param lastErrorAt            instant of the last failed attempt
 */
public record SyncStatusResponse(long appliedVersion,
                                 long snapshotVersion,
                                 Long currentVersion,
                                 Long oldestAvailableVersion,
                                 String lastSyncResult,
                                 Instant lastSyncAt,
                                 String lastError,
                                 Instant lastErrorAt) {
}
