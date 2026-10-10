package ar.edu.um.turnos.catalog.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalPage;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.um.turnos.catalog.support.CatalogFixtures;
import ar.edu.um.turnos.catalog.support.CatedraRedisFixture;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Incremental application of the versions published in Redis (sections 3.2, 6 and 10.1 of the
 * statement, sections 14 and 18.2 of the reference).
 *
 * <p>The feed is published in a real Redis container through {@link CatedraRedisFixture} and
 * read by the real adapter; only the trace of the reads is recorded, because "the copy was
 * rebuilt" and "the missing delta was never asked for" are different claims.</p>
 */
class IncrementalSyncApiTest extends CatalogApiIntegrationTest {

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private CatalogSyncRepository syncRepository;

    @Test
    void pendingVersionsAreAppliedInOrderAndTheStatusReportsTheObservedWindow() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        // Version 4 publishes a professional that does not exist locally yet, version 5
        // changes another one that does: after the run both effects must be visible.
        CatedraRedisFixture.publishWindow(5, 1);
        CatedraRedisFixture.publishProfessional(106, CatalogFixtures.HEALTH_CATEGORY,
                "Nueva", "Profesional", true);
        CatedraRedisFixture.publishChanges(4, 106L);
        CatedraRedisFixture.publishProfessional(CatalogFixtures.ANA_PEREZ,
                CatalogFixtures.HEALTH_CATEGORY, "Ana", "PerezQuinta", true);
        CatedraRedisFixture.publishChanges(5, CatalogFixtures.ANA_PEREZ);

        runSync()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(5))
                .andExpect(jsonPath("$.snapshotVersion").value(5))
                .andExpect(jsonPath("$.lastSyncResult").value("INCREMENTAL_APPLIED"))
                .andExpect(jsonPath("$.currentVersion").value(5))
                .andExpect(jsonPath("$.oldestAvailableVersion").value(1))
                .andExpect(jsonPath("$.lastError").doesNotExist());

        // Both deltas were read, and they were read in the order of the versions.
        assertThat(incrementalGateway.changesVersionsRequested()).containsExactly(4L, 5L);

        // The professional of version 4 arrived, and so did the one of version 5.
        assertThat(professional(106).getFirstName()).isEqualTo("Nueva");
        assertThat(professional(CatalogFixtures.ANA_PEREZ).getLastName()).isEqualTo("PerezQuinta");
        assertThat(professionals().content()).hasSize(6);

        // The breadcrumb of the private namespace of this project was written.
        assertThat(CatedraRedisFixture.lastSyncDiagnostic()).hasValueSatisfying(diagnostic -> {
            assertThat(diagnostic).contains("\"result\":\"INCREMENTAL_APPLIED\"");
            assertThat(diagnostic).contains("\"appliedVersion\":5");
            assertThat(diagnostic).contains("\"currentVersion\":5");
        });
    }

    @Test
    void applyingTheSameVersionTwiceProducesTheSameRows() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        Professional published = professional(CatalogFixtures.ANA_PEREZ)
                .toBuilder().lastName("PerezReapplied").build();

        syncRepository.applyIncremental(4, List.of(), List.of(published), List.of());
        long totalAfterFirst = professionals().totalElements();
        String lastNameAfterFirst = professional(CatalogFixtures.ANA_PEREZ).getLastName();

        // The redelivery of version 4 writes the same remote id, so it rewrites the same row
        // instead of duplicating it.
        syncRepository.applyIncremental(4, List.of(), List.of(published), List.of());

        assertThat(professionals().totalElements()).isEqualTo(totalAfterFirst);
        assertThat(professional(CatalogFixtures.ANA_PEREZ).getLastName())
                .isEqualTo(lastNameAfterFirst)
                .isEqualTo("PerezReapplied");
        assertThat(syncRepository.findState()).hasValueSatisfying(state -> {
            assertThat(state.getAppliedVersion()).isEqualTo(4L);
            assertThat(state.getLastSyncResult().name()).isEqualTo("INCREMENTAL_APPLIED");
            assertThat(state.getLastError()).isNull();
        });
    }

    @Test
    void runningTheSyncTwiceWhenNothingIsPendingReadsTheDeltaOnlyOnce() throws Exception {
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        CatedraRedisFixture.publishWindow(4, 1);
        CatedraRedisFixture.publishProfessional(CatalogFixtures.ANA_PEREZ,
                CatalogFixtures.HEALTH_CATEGORY, "Ana", "PerezCuarta", true);
        CatedraRedisFixture.publishChanges(4, CatalogFixtures.ANA_PEREZ);

        runSync()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(4))
                .andExpect(jsonPath("$.lastSyncResult").value("INCREMENTAL_APPLIED"));
        assertThat(incrementalGateway.changesVersionsRequested()).containsExactly(4L);

        // The same version again: nothing is pending, so no delta is read and no row moves.
        runSync()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(4))
                .andExpect(jsonPath("$.lastSyncResult").value("ALREADY_CURRENT"))
                .andExpect(jsonPath("$.lastError").doesNotExist());

        assertThat(incrementalGateway.changesVersionsRequested()).containsExactly(4L);
        assertThat(professional(CatalogFixtures.ANA_PEREZ).getLastName()).isEqualTo("PerezCuarta");
        assertThat(professionals().content()).hasSize(5);
    }

    @Test
    void aLocalVersionOutsideTheWindowFallsBackToASnapshotWithoutReadingTheMissingDelta()
            throws Exception {
        // Local copy at version 3 while the catedra only keeps the deltas of 4..7: the link
        // between 3 and 4 cannot be proven, so the copy is rebuilt as a whole.
        syncFrom(CatalogFixtures.snapshot(3)).andExpect(status().isOk());

        CatedraRedisFixture.publishWindow(7, 4);
        CatedraRedisFixture.publishProfessional(106, CatalogFixtures.HEALTH_CATEGORY,
                "Nueva", "Profesional", true);
        CatedraRedisFixture.publishChanges(5, 106L);
        CatedraRedisFixture.publishChanges(6, 106L);
        CatedraRedisFixture.publishChanges(7, 106L);
        // changes:4 is deliberately NOT published: the catedra no longer keeps it.
        snapshotGateway.returns(CatalogFixtures.snapshot(7));
        int snapshotsBeforeTheRun = snapshotGateway.fetchCount();

        runSync()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(7))
                .andExpect(jsonPath("$.snapshotVersion").value(7))
                .andExpect(jsonPath("$.lastSyncResult").value("SNAPSHOT_APPLIED"))
                .andExpect(jsonPath("$.currentVersion").value(7))
                .andExpect(jsonPath("$.oldestAvailableVersion").value(4))
                .andExpect(jsonPath("$.lastError").doesNotExist());

        // The delta of the missing version was never requested, and neither were the ones
        // that were available: the snapshot replaced the copy instead.
        assertThat(incrementalGateway.changesRequestedFor(4)).isFalse();
        assertThat(incrementalGateway.changesVersionsRequested()).isEmpty();
        assertThat(snapshotGateway.fetchCount())
                .as("exactly one snapshot: the fallback of this run, no more")
                .isEqualTo(snapshotsBeforeTheRun + 1);
        assertThat(professionals().content()).hasSize(5);
        assertThat(catalogRepository.findProfessionalById(106L))
                .as("the professional announced by the discarded deltas must not be local")
                .isEmpty();
    }

    private Professional professional(long id) {
        return catalogRepository.findProfessionalById(id)
                .orElseThrow(() -> new AssertionError("professional " + id + " is not local"))
                .professional();
    }

    private ProfessionalPage professionals() {
        return catalogRepository.searchProfessionals(new ProfessionalSearchCriteria(
                null, null, null, null, 0, 100, null, null));
    }
}
