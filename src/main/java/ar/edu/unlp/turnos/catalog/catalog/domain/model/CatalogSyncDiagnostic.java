package ar.edu.unlp.turnos.catalog.catalog.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * Diagnostic written to the private namespace of this project,
 * {@code alumnos:{groupId}:diagnostics:last-sync} (contract v1, section 14.6).
 *
 * <p>It is an <strong>auxiliary</strong> breadcrumb for the operator: it never feeds a
 * decision of the synchronization and it is never a source of truth. When Redis cannot be
 * written the synchronization still succeeds; only this record is lost.</p>
 */
@Getter
@Builder
public class CatalogSyncDiagnostic {

    /** Version of the local copy when the attempt finished. */
    private final long appliedVersion;

    /** Window observed in Redis, {@code null} when it could not be read. */
    private final Long currentVersion;
    private final Long oldestAvailableVersion;

    /** Outcome of the attempt, {@code null} when it did not finish. */
    private final String result;

    /** Self contained description of a failure, {@code null} on success. */
    private final String detail;

    private final Instant recordedAt;

    /**
     * @param state      state of the synchronization after the attempt
     * @param recordedAt instant the diagnostic is written
     * @return the diagnostic describing that state
     */
    public static CatalogSyncDiagnostic from(CatalogSyncState state, Instant recordedAt) {
        return CatalogSyncDiagnostic.builder()
                .appliedVersion(state.getAppliedVersion())
                .currentVersion(state.getCurrentVersion())
                .oldestAvailableVersion(state.getOldestAvailableVersion())
                .result(state.getLastSyncResult() == null ? null : state.getLastSyncResult().name())
                .detail(state.getLastError())
                .recordedAt(recordedAt)
                .build();
    }
}
