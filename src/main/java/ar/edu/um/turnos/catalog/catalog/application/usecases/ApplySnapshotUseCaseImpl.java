package ar.edu.um.turnos.catalog.catalog.application.usecases;

import ar.edu.um.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.ApplySnapshotUseCase;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Applies a complete snapshot to the local copy.
 *
 * <p>Responsibilities: reject a snapshot that does not match the contract (recording the
 * failed attempt) and delegate the atomic write to the outbound port. The validation rules
 * themselves live in {@link CatalogSnapshot#validate()}, next to the model they protect, so
 * the local copy cannot be corrupted by any other entry point (HTTP, startup routine, test).</p>
 *
 * <p>There is no facade above this use case: the controllers inject the use case ports
 * directly, because a facade that only delegates would add nothing (anti-pattern 3).</p>
 */
@Component
@RequiredArgsConstructor
public class ApplySnapshotUseCaseImpl implements ApplySnapshotUseCase {

    private static final Logger log = LoggerFactory.getLogger(ApplySnapshotUseCaseImpl.class);

    private final CatalogSyncRepository syncRepository;

    @Override
    public CatalogSyncState apply(CatalogSnapshot snapshot) {
        if (snapshot == null) {
            recordFailure(null, "The snapshot to apply is empty.");
            throw CatalogException.invalidSnapshot("The snapshot to apply is empty.");
        }
        try {
            snapshot.validate();
        } catch (IllegalArgumentException rejected) {
            recordFailure(snapshot.getSnapshotVersion(), rejected.getMessage());
            throw CatalogException.invalidSnapshot(rejected.getMessage());
        }

        CatalogSyncState state = syncRepository.applySnapshot(snapshot);
        log.info("Catalog snapshot applied: version={}, categories={}, professionals={}, schedules={}",
                state.getAppliedVersion(),
                snapshot.getProfessionalCategories().size(),
                snapshot.getProfessionals().size(),
                snapshot.getWeeklySchedules().size());
        return state;
    }

    /**
     * Keeps the failure visible through {@code GET /api/sync/status} without letting a
     * problem of the state table hide the real reason of the rejection.
     */
    private void recordFailure(Long attemptedVersion, String error) {
        try {
            syncRepository.recordFailure(attemptedVersion, error, Instant.now());
        } catch (RuntimeException recordingFailed) {
            log.warn("The failed snapshot attempt could not be recorded: {}", recordingFailed.getMessage());
        }
    }
}
