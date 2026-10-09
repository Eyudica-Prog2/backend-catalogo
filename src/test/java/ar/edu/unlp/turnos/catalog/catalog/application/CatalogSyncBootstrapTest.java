package ar.edu.unlp.turnos.catalog.catalog.application;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.RunFullSyncUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test of the routine that initializes an empty local copy during startup.
 *
 * <p>Two decisions are under test: the local copy is considered empty when no snapshot was
 * ever applied, and a failure of the synchronization must never prevent the service from
 * booting (section 4.1 of the statement).</p>
 *
 * <p>The state is read through {@code CatalogSyncRepository}, the same outbound port the
 * application uses: the bootstrap must not depend on the HTTP layer to know whether the copy
 * exists, otherwise a service without a client could never start.</p>
 */
@ExtendWith(MockitoExtension.class)
class CatalogSyncBootstrapTest {

    @Mock
    private CatalogSyncRepository syncRepository;

    @Mock
    private RunFullSyncUseCase runFullSync;

    @InjectMocks
    private CatalogSyncBootstrap bootstrap;

    @Test
    void runsTheSynchronizationWhenNoSnapshotWasEverApplied() {
        when(syncRepository.findState()).thenReturn(Optional.empty());

        bootstrap.run(null);

        verify(runFullSync).runFullSync();
    }

    @Test
    void skipsWhenTheLocalCopyIsAlreadyInitialized() {
        when(syncRepository.findState()).thenReturn(Optional.of(CatalogSyncState.builder()
                .snapshotVersion(7L)
                .appliedVersion(7L)
                .build()));

        bootstrap.run(null);

        verify(runFullSync, never()).runFullSync();
    }

    @Test
    void bootsEvenWhenTheCatedraIsNotAvailable() {
        when(syncRepository.findState()).thenReturn(Optional.empty());
        when(runFullSync.runFullSync())
                .thenThrow(CatedraException.unavailable("The catedra API is not available."));

        assertThatNoException().isThrownBy(() -> bootstrap.run(null));
    }

    @Test
    void bootsEvenWhenTheSynchronizationFailsUnexpectedly() {
        when(syncRepository.findState()).thenReturn(Optional.empty());
        when(runFullSync.runFullSync()).thenThrow(new IllegalStateException("boom"));

        assertThatNoException().isThrownBy(() -> bootstrap.run(null));
    }
}
