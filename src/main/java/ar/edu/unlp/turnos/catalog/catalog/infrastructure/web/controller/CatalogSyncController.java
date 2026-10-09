package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.controller;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.GetSyncStatusUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunFullSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunIncrementalSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.SyncStatusResponse;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.mapper.SyncDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Status and control of the catalog synchronization (section 4.1 of the statement).
 *
 * <p>{@code GET /api/sync/status} reports what the local copy holds and what the last attempt
 * left behind; {@code POST /api/internal/sync/force} triggers a full synchronization on
 * demand and {@code POST /api/internal/sync/run} applies the versions Redis published since
 * the local copy, falling back to a snapshot when the link between versions cannot be proven.
 * Neither of them rebuilds a message: the state they return is produced by the use cases and
 * any failure reaches the shared error handler.</p>
 */
@RestController
@RequiredArgsConstructor
public class CatalogSyncController {

    private final GetSyncStatusUseCase getSyncStatus;
    private final RunFullSyncUseCase runFullSync;
    private final RunIncrementalSyncUseCase runIncrementalSync;
    private final SyncDtoMapper mapper;

    /**
     * Reports the state of the synchronization.
     *
     * @return 200 with the state; version zero with no error when this service never
     *         synchronized
     */
    @GetMapping("/api/sync/status")
    public ResponseEntity<SyncStatusResponse> getSyncStatus() {
        return ResponseEntity.ok(mapper.toResponse(getSyncStatus.getSyncStatus()));
    }

    /**
     * Runs a full synchronization on demand: downloads the snapshot from the catedra and
     * replaces the local copy with it.
     *
     * <p>The response is the state after the attempt. A failure of the download is answered
     * as {@code 503 CATEDRA_*} and a snapshot that does not match the contract as
     * {@code 503 SNAPSHOT_INVALID}; in both cases the local copy keeps serving the data it
     * already had and the failure becomes visible through {@code GET /api/sync/status}.</p>
     *
     * @return 200 with the state of the synchronization
     */
    @PostMapping("/api/internal/sync/force")
    public ResponseEntity<SyncStatusResponse> forceSync() {
        CatalogSyncState state = runFullSync.runFullSync();
        return ResponseEntity.ok(mapper.toResponse(state));
    }

    /**
     * Applies what the catedra published in Redis since the local copy: the pending
     * versions in order, or a complete snapshot when the link between versions cannot be
     * proven (contract v1, section 18.2).
     *
     * <p>This is the on demand version of the same routine the Kafka consumer runs and the
     * safety net of the periodic catch up runs: the three share one use case, so an
     * operator, a notification and a scheduler can never disagree about what "apply the
     * pending changes" means.</p>
     *
     * <p>A failure of the cache or of the fallback snapshot is answered as {@code 503
     * CATEDRA_*}; the local copy keeps serving the data it already had and the failure
     * becomes visible through {@code GET /api/sync/status}.</p>
     *
     * @return 200 with the state of the synchronization after the attempt
     */
    @PostMapping("/api/internal/sync/run")
    public ResponseEntity<SyncStatusResponse> runSync() {
        CatalogSyncState state = runIncrementalSync.runIncrementalSync();
        return ResponseEntity.ok(mapper.toResponse(state));
    }
}
