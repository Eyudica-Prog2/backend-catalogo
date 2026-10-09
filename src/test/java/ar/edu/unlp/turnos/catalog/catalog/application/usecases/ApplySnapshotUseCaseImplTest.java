package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the use case that applies a snapshot.
 *
 * <p>The rule it protects: a snapshot that does not match the contract never reaches the
 * local copy, the failed attempt is recorded and the applied version stays where it was.</p>
 */
@ExtendWith(MockitoExtension.class)
class ApplySnapshotUseCaseImplTest {

    @Mock
    private CatalogSyncRepository syncRepository;

    @InjectMocks
    private ApplySnapshotUseCaseImpl useCase;

    @Test
    void appliesAValidSnapshotAndReturnsTheStateThatWasStored() {
        CatalogSnapshot snapshot = CatalogFixtures.snapshot(7);
        CatalogSyncState applied = CatalogSyncState.builder()
                .snapshotVersion(7L)
                .appliedVersion(7L)
                .lastSyncAt(Instant.parse("2026-07-01T10:00:00Z"))
                .build();
        when(syncRepository.applySnapshot(snapshot)).thenReturn(applied);

        assertThat(useCase.apply(snapshot)).isSameAs(applied);

        verify(syncRepository).applySnapshot(snapshot);
        verify(syncRepository, never()).recordFailure(any(), any(), any());
    }

    @Test
    void rejectsASnapshotWithAnUnknownCategoryWithoutTouchingTheLocalCopy() {
        CatalogSnapshot snapshot = CatalogFixtures.snapshotWithUnknownCategory(6);

        assertThatThrownBy(() -> useCase.apply(snapshot))
                .isInstanceOfSatisfying(CatalogException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SNAPSHOT_INVALID");
                    assertThat(exception.getHttpStatus()).isEqualTo(503);
                    assertThat(exception.getDetail())
                            .contains("999")
                            .contains("not part of the snapshot");
                });

        verify(syncRepository, never()).applySnapshot(any());
        verify(syncRepository).recordFailure(eq(6L), contains("999"), any(Instant.class));
    }

    @Test
    void rejectsASnapshotWithAnInconsistentTimeRange() {
        CatalogSnapshot snapshot = CatalogFixtures.snapshotWithBrokenTimeRange(3);

        assertThatThrownBy(() -> useCase.apply(snapshot))
                .isInstanceOfSatisfying(CatalogException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SNAPSHOT_INVALID");
                    assertThat(exception.getDetail()).contains("start before it ends");
                });

        verify(syncRepository, never()).applySnapshot(any());
    }

    @Test
    void rejectsAnEmptySnapshot() {
        assertThatThrownBy(() -> useCase.apply(null))
                .isInstanceOfSatisfying(CatalogException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("SNAPSHOT_INVALID");
                    assertThat(exception.getHttpStatus()).isEqualTo(503);
                });

        verify(syncRepository, never()).applySnapshot(any());
    }

    @Test
    void keepsTheRejectionWhenTheFailureCannotBeRecorded() {
        CatalogSnapshot snapshot = CatalogFixtures.snapshotWithUnknownCategory(6);
        doThrow(new IllegalStateException("state table unavailable"))
                .when(syncRepository).recordFailure(any(), any(), any());

        // A problem while writing the state must not hide the real reason of the rejection.
        assertThatThrownBy(() -> useCase.apply(snapshot))
                .isInstanceOfSatisfying(CatalogException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo("SNAPSHOT_INVALID"));

        verify(syncRepository, never()).applySnapshot(any());
    }
}
