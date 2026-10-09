package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests of the use case that reports the state of the synchronization.
 */
@ExtendWith(MockitoExtension.class)
class GetSyncStatusUseCaseImplTest {

    @Mock
    private CatalogSyncRepository syncRepository;

    @InjectMocks
    private GetSyncStatusUseCaseImpl useCase;

    @Test
    void returnsTheStoredState() {
        CatalogSyncState stored = CatalogSyncState.builder()
                .snapshotVersion(12L)
                .appliedVersion(11L)
                .lastSyncAt(Instant.parse("2026-07-01T10:00:00Z"))
                .lastError("The snapshot published by the catedra could not be parsed.")
                .lastErrorAt(Instant.parse("2026-07-01T10:05:00Z"))
                .build();
        when(syncRepository.findState()).thenReturn(Optional.of(stored));

        assertThat(useCase.getSyncStatus()).isSameAs(stored);
    }

    @Test
    void answersVersionZeroWithoutErrorWhenThisServiceNeverSynchronized() {
        when(syncRepository.findState()).thenReturn(Optional.empty());

        CatalogSyncState state = useCase.getSyncStatus();

        assertThat(state.getAppliedVersion()).isZero();
        assertThat(state.getSnapshotVersion()).isZero();
        assertThat(state.isSynced()).isFalse();
        assertThat(state.hasError()).isFalse();
    }
}
