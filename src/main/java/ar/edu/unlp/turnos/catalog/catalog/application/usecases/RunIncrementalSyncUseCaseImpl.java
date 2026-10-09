package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogChanges;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncDiagnostic;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.SyncResult;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunFullSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunIncrementalSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogIncrementalGateway;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncDiagnostics;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.shared.error.ApiException;
import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies the versions published in Redis that this service has not applied yet.
 *
 * <p>The algorithm, one decision per situation (contract v1, sections 14.2, 14.5, 16 and
 * 18.2):</p>
 *
 * <ol>
 *   <li>read {@code current-version} and {@code oldest-available-version};</li>
 *   <li>local version equals the published one &rarr; nothing to do;</li>
 *   <li>local version outside the window (older than {@code oldestAvailableVersion}, or
 *       newer than {@code currentVersion}, or no local version at all) &rarr; the link
 *       between versions cannot be proven, so a <strong>complete snapshot</strong> rebuilds
 *       the copy. The connecting {@code changes:{version}} is never even read;</li>
 *   <li>otherwise apply {@code local + 1 ... current}, one version at a time: the delta
 *       announces ids, the current state of each id is read from Redis, and the local
 *       version only advances inside the same transaction that writes those changes.</li>
 * </ol>
 *
 * <p>Every failure is recorded (without moving the applied version) so it becomes visible
 * through {@code GET /api/sync/status}, and a breadcrumb is written to the private Redis
 * namespace of this project (never as a source of truth).</p>
 */
@Component
@RequiredArgsConstructor
public class RunIncrementalSyncUseCaseImpl implements RunIncrementalSyncUseCase {

    private static final Logger log = LoggerFactory.getLogger(RunIncrementalSyncUseCaseImpl.class);

    private final CatalogSyncRepository syncRepository;
    private final CatalogIncrementalGateway incrementalGateway;
    private final CatalogSyncDiagnostics diagnostics;
    private final RunFullSyncUseCase runFullSync;

    @Override
    public CatalogSyncState runIncrementalSync() {
        CatalogVersionWindow window = fetchWindow();
        observeWindow(window);

        CatalogSyncState before = currentState();
        if (!before.isSynced() || !window.canBeFollowedFrom(before.getAppliedVersion())) {
            log.info("Local copy cannot be continued incrementally: appliedVersion={}, window=[{}..{}]. "
                            + "Rebuilding it from a complete snapshot.",
                    before.getAppliedVersion(), window.getOldestAvailableVersion(),
                    window.getCurrentVersion());
            return snapshot();
        }
        if (before.getAppliedVersion() == window.getCurrentVersion()) {
            syncRepository.recordSuccess(SyncResult.ALREADY_CURRENT, Instant.now());
            return writeDiagnostic(currentState());
        }
        return applyPendingVersions(before, window);
    }

    /**
     * Reads the published window. Redis is the only source of this information, so a
     * failure here leaves the local version untouched and is reported as an unavailable
     * integration instead of an internal error.
     */
    private CatalogVersionWindow fetchWindow() {
        CatalogVersionWindow window;
        try {
            window = incrementalGateway.fetchVersionWindow();
        } catch (RuntimeException unavailable) {
            recordFailure(null, describe(unavailable));
            writeDiagnostic(currentState());
            throw unavailable;
        }
        try {
            window.validate();
        } catch (IllegalArgumentException invalidWindow) {
            CatedraException rejected = CatedraException.invalidResponse(invalidWindow.getMessage(),
                    invalidWindow);
            recordFailure(null, rejected.getDetail());
            writeDiagnostic(currentState());
            throw rejected;
        }
        return window;
    }

    /**
     * Rebuilds the copy from the complete snapshot. The failure of that fallback is already
     * recorded by the snapshot use case; recording it again here would replace the precise
     * reason with a generic one.
     */
    private CatalogSyncState snapshot() {
        CatalogSyncState state = runFullSync.runFullSync();
        writeDiagnostic(state);
        return state;
    }

    /**
     * Applies {@code local + 1} up to {@code currentVersion}, in order. Each version is one
     * transaction: a failure in the middle leaves the applied version at the last version
     * that was fully written, and the next attempt resumes exactly from there.
     */
    private CatalogSyncState applyPendingVersions(CatalogSyncState before, CatalogVersionWindow window) {
        long attempted = before.getAppliedVersion();
        try {
            while (attempted < window.getCurrentVersion()) {
                attempted++;
                ResolvedChanges changes = resolve(attempted);
                syncRepository.applyIncremental(attempted, changes.categories(),
                        changes.professionals(), changes.schedules());
                log.info("Incremental catalog version applied: version={}", attempted);
            }
        } catch (RuntimeException failure) {
            recordFailure(attempted, describe(failure));
            writeDiagnostic(currentState());
            throw failure;
        }
        syncRepository.recordSuccess(SyncResult.INCREMENTAL_APPLIED, Instant.now());
        return writeDiagnostic(currentState());
    }

    /**
     * Turns the ids of one delta into the current state published for each of them. An id
     * that is no longer published cannot be applied blindly (it would either resurrect a
     * deleted record or silently keep a stale one), so the version is refused and a snapshot
     * becomes the way forward.
     */
    private ResolvedChanges resolve(long version) {
        CatalogChanges changes = incrementalGateway.fetchChanges(version)
                .orElseThrow(() -> CatedraException.invalidResponse(
                        "Redis does not publish catedra:sync:changes:" + version
                                + " anymore; the local copy must be rebuilt from a snapshot."));
        try {
            changes.validateFor(version);
        } catch (IllegalArgumentException mismatched) {
            throw CatedraException.invalidResponse(mismatched.getMessage(), mismatched);
        }

        List<ProfessionalCategory> categories = changes.getProfessionalCategoryIds().stream()
                .map(id -> incrementalGateway.fetchCategory(id)
                        .orElseThrow(() -> missingRecord("professional category", id, version)))
                .toList();
        List<Professional> professionals = changes.getProfessionalIds().stream()
                .map(id -> incrementalGateway.fetchProfessional(id)
                        .orElseThrow(() -> missingRecord("professional", id, version)))
                .toList();
        List<WeeklySchedule> schedules = changes.getWeeklyScheduleIds().stream()
                .map(id -> incrementalGateway.fetchWeeklySchedule(id)
                        .orElseThrow(() -> missingRecord("weekly schedule", id, version)))
                .toList();
        return new ResolvedChanges(categories, professionals, schedules);
    }

    private CatedraException missingRecord(String entity, long id, long version) {
        return CatedraException.invalidResponse(
                "Version " + version + " affects the " + entity + " " + id
                        + ", which is no longer published in Redis; the local copy must be "
                        + "rebuilt from a snapshot.");
    }

    private void observeWindow(CatalogVersionWindow window) {
        try {
            syncRepository.recordVersionWindow(window);
        } catch (RuntimeException recordingFailed) {
            log.warn("The observed version window could not be stored: {}", recordingFailed.getMessage());
        }
    }

    /**
     * Keeps a failed attempt visible through {@code GET /api/sync/status} without letting a
     * problem of the state table hide the original error.
     */
    private void recordFailure(Long attemptedVersion, String detail) {
        try {
            syncRepository.recordFailure(attemptedVersion, detail, Instant.now());
        } catch (RuntimeException recordingFailed) {
            log.warn("The failed synchronization could not be recorded: {}", recordingFailed.getMessage());
        }
    }

    /**
     * Writes the breadcrumb of the private namespace. A failure here is a warning and never
     * changes the outcome of the synchronization.
     */
    private CatalogSyncState writeDiagnostic(CatalogSyncState state) {
        try {
            diagnostics.record(CatalogSyncDiagnostic.from(state, Instant.now()));
        } catch (RuntimeException recordingFailed) {
            log.warn("The synchronization diagnostic could not be written: {}",
                    recordingFailed.getMessage());
        }
        return state;
    }

    private CatalogSyncState currentState() {
        return syncRepository.findState().orElseGet(CatalogSyncState::notSynced);
    }

    /**
     * @param failure any failure raised while reading Redis
     * @return its self contained description, never null
     */
    private static String describe(RuntimeException failure) {
        if (failure instanceof ApiException apiException) {
            return apiException.getDetail();
        }
        return failure.getMessage() == null
                ? "The incremental synchronization failed: " + failure.getClass().getSimpleName()
                : failure.getMessage();
    }

    /**
     * Current state of every id affected by one version, ready to be written together.
     */
    private record ResolvedChanges(List<ProfessionalCategory> categories,
                                   List<Professional> professionals,
                                   List<WeeklySchedule> schedules) {
    }
}
