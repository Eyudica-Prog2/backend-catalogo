package ar.edu.unlp.turnos.catalog.catalog.domain.model;

/**
 * Outcome of the last synchronization attempt, as reported by {@code GET /api/sync/status}.
 *
 * <p>Stored as text in {@code catalog_sync_state.last_sync_result} (the values are part of a
 * check constraint) and exposed as a string in the status body, so clients never have to
 * parse an internal type.</p>
 */
public enum SyncResult {

    /** A complete snapshot was applied: the local copy was replaced as a whole. */
    SNAPSHOT_APPLIED,

    /** One or more incremental versions were applied in order. */
    INCREMENTAL_APPLIED,

    /** Nothing was pending: the local copy already matched the published version. */
    ALREADY_CURRENT,

    /** The attempt failed; the reason is in {@code lastError} and no version advanced. */
    FAILED
}
