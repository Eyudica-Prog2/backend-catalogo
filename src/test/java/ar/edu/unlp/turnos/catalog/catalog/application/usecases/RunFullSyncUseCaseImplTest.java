package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.ApplySnapshotUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSnapshotGateway;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the use case that runs a full synchronization.
 *
 * <p>The rule it protects: a failure of the download is recorded (without touching the local
 * copy nor the versions) and then propagated with its original functional code.</p>
 */
@ExtendWith(MockitoExtension.class)
class RunFullSyncUseCaseImplTest {

    @Mock
    private CatalogSnapshotGateway snapshotGateway;

    @Mock
    private ApplySnapshotUseCase applySnapshot;

    @Mock
    private CatalogSyncRepository syncRepository;

    @InjectMocks
    private RunFullSyncUseCaseImpl useCase;

    @Test
    void downloadsTheSnapshotAndAppliesIt() {
        CatalogSnapshot snapshot = CatalogFixtures.snapshot(9);
        CatalogSyncState applied = CatalogSyncState.builder()
                .snapshotVersion(9L)
                .appliedVersion(9L)
                .build();
        when(snapshotGateway.fetchCatalogSnapshot()).thenReturn(snapshot);
        when(applySnapshot.apply(snapshot)).thenReturn(applied);

        assertThat(useCase.runFullSync()).isSameAs(applied);

        verify(syncRepository, never()).recordFailure(any(), any(), any());
    }

    @Test
    void recordsAndPropagatesACatedraFailureWithItsOwnCode() {
        CatedraException failure = CatedraException.unavailable("The catedra API is not available.");
        when(snapshotGateway.fetchCatalogSnapshot()).thenThrow(failure);

        assertThatThrownBy(useCase::runFullSync).isSameAs(failure);

        verify(syncRepository).recordFailure(isNull(), contains("not available"), any(Instant.class));
        verify(applySnapshot, never()).apply(any());
    }

    @Test
    void recordsAnUnexpectedFailureWithASelfContainedMessage() {
        IllegalStateException failure = new IllegalStateException("connection reset by peer");
        when(snapshotGateway.fetchCatalogSnapshot()).thenThrow(failure);

        assertThatThrownBy(useCase::runFullSync).isSameAs(failure);

        verify(syncRepository).recordFailure(isNull(), contains("could not be downloaded"),
                any(Instant.class));
        verify(applySnapshot, never()).apply(any());
    }
}
