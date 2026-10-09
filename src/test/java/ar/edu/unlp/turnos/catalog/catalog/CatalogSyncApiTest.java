package ar.edu.unlp.turnos.catalog.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unlp.turnos.catalog.shared.error.CatedraException;
import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * End to end tests of the synchronization: status, forced synchronization, atomicity of the
 * application and the way failures are reported (sections 4.1 and 5 of the statement).
 *
 * <p>Every test starts from an empty local copy because the snapshot gateway is a stub and
 * each test runs in a transaction that is rolled back.</p>
 */
class CatalogSyncApiTest extends CatalogApiIntegrationTest {

    @Test
    void syncStatusWithoutTokenIsRejectedWith401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/sync/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void forceSyncWithoutTokenIsRejectedWith401ProblemJson() throws Exception {
        mockMvc.perform(post("/api/internal/sync/force"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void statusBeforeTheFirstSyncReportsVersionZeroAndTheStartupFailure() throws Exception {
        // The startup routine already tried to initialize the local copy and the catedra was
        // not available: the failure must be visible, but no version may have moved.
        mockMvc.perform(get("/api/sync/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.appliedVersion").value(0))
                .andExpect(jsonPath("$.snapshotVersion").value(0))
                .andExpect(jsonPath("$.lastError").isNotEmpty())
                .andExpect(jsonPath("$.lastErrorAt").isNotEmpty())
                .andExpect(jsonPath("$.lastSyncAt").doesNotExist());
    }

    @Test
    void forcedSyncAppliesTheSnapshotAndAdvancesTheVersion() throws Exception {
        syncFrom(CatalogFixtures.snapshot(7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(7))
                .andExpect(jsonPath("$.snapshotVersion").value(7))
                .andExpect(jsonPath("$.lastError").doesNotExist())
                .andExpect(jsonPath("$.lastSyncAt").isNotEmpty());

        // The three collections are readable through the public API of the slice.
        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"));

        mockMvc.perform(get("/api/professionals/" + CatalogFixtures.ANA_PEREZ)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules.length()").value(2));

        mockMvc.perform(get("/api/professional-categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void forcedSyncReplacesTheCopyInsteadOfAppendingToIt() throws Exception {
        syncFrom(CatalogFixtures.snapshot(7)).andExpect(status().isOk());
        syncFrom(CatalogFixtures.snapshot(8)).andExpect(status().isOk());

        // Applying a second snapshot must leave exactly the records of the new one.
        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"));

        mockMvc.perform(get("/api/professionals/" + CatalogFixtures.ANA_PEREZ)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules.length()").value(2));

        mockMvc.perform(get("/api/sync/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(8));
    }

    @Test
    void applyingTheSameSnapshotTwiceLeavesTheSameData() throws Exception {
        syncFrom(CatalogFixtures.snapshot(7)).andExpect(status().isOk());
        syncFrom(CatalogFixtures.snapshot(7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(7))
                .andExpect(jsonPath("$.lastError").doesNotExist());

        // The second application replaces the copy instead of appending to it.
        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"));

        mockMvc.perform(get("/api/professionals/" + CatalogFixtures.ANA_PEREZ)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules.length()").value(2));

        mockMvc.perform(get("/api/professional-categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void invalidSnapshotIsRejectedWithoutAdvancingTheAppliedVersion() throws Exception {
        syncFrom(CatalogFixtures.snapshot(5)).andExpect(status().isOk());

        snapshotGateway.returns(CatalogFixtures.snapshotWithUnknownCategory(6));
        forceSync()
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(ErrorCodes.SNAPSHOT_INVALID))
                .andExpect(jsonPath("$.detail").isNotEmpty());

        // The local copy still holds version 5, the attempted version 6 is visible with its error.
        mockMvc.perform(get("/api/sync/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(5))
                .andExpect(jsonPath("$.snapshotVersion").value(6))
                .andExpect(jsonPath("$.lastError").isNotEmpty())
                .andExpect(jsonPath("$.lastErrorAt").isNotEmpty());

        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"));
    }

    @Test
    void snapshotWithAnInconsistentTimeRangeIsRejectedAsInvalid() throws Exception {
        snapshotGateway.returns(CatalogFixtures.snapshotWithBrokenTimeRange(3));

        forceSync()
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(ErrorCodes.SNAPSHOT_INVALID))
                .andExpect(jsonPath("$.detail", containsString("start before it ends")));

        mockMvc.perform(get("/api/sync/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(0));
    }

    @Test
    void failedDownloadIsReportedAs503AndKeepsTheLocalCopy() throws Exception {
        syncFrom(CatalogFixtures.snapshot(5)).andExpect(status().isOk());

        snapshotGateway.fails(CatedraException.unavailable("The catedra API is not available."));
        forceSync()
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.CATEDRA_UNAVAILABLE));

        // No version was received, so neither version moved and only the error is recorded.
        mockMvc.perform(get("/api/sync/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(5))
                .andExpect(jsonPath("$.snapshotVersion").value(5))
                .andExpect(jsonPath("$.lastError").isNotEmpty());

        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"));
    }

    @Test
    void aSuccessfulSyncClearsThePreviousError() throws Exception {
        syncFrom(CatalogFixtures.snapshot(5)).andExpect(status().isOk());
        snapshotGateway.returns(CatalogFixtures.snapshotWithUnknownCategory(6));
        forceSync().andExpect(status().isServiceUnavailable());

        syncFrom(CatalogFixtures.snapshot(7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(7))
                .andExpect(jsonPath("$.lastError").doesNotExist())
                .andExpect(jsonPath("$.lastErrorAt").doesNotExist());

        assertThat(snapshotGateway.fetchCount()).isEqualTo(3);
    }
}
