package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.mapper;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.SyncResult;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.SyncStatusResponse;
import org.springframework.stereotype.Component;

/**
 * Maps the synchronization state into the body of {@code GET /api/sync/status} and of the
 * on demand synchronizations.
 *
 * <p>The state is always present: a service that never synchronized reports version zero
 * instead of an empty body, so the client can show "pending" as a normal state. The version
 * window and the outcome of the last attempt are only reported when they were actually
 * observed: an unknown window is omitted, never guessed.</p>
 */
@Component
public class SyncDtoMapper {

    /**
     * @param state current state, may be null
     * @return the status body, or null when the state is null
     */
    public SyncStatusResponse toResponse(CatalogSyncState state) {
        if (state == null) {
            return null;
        }
        return new SyncStatusResponse(
                state.getAppliedVersion(),
                state.getSnapshotVersion(),
                state.getCurrentVersion(),
                state.getOldestAvailableVersion(),
                resultName(state.getLastSyncResult()),
                state.getLastSyncAt(),
                state.getLastError(),
                state.getLastErrorAt());
    }

    private static String resultName(SyncResult result) {
        return result == null ? null : result.name();
    }
}
