package ar.edu.um.turnos.catalog.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.turnos.catalog.CatalogApplication;
import ar.edu.um.turnos.catalog.support.CatalogFixtures;
import ar.edu.um.turnos.catalog.support.PreloadedCatalogSnapshotConfiguration;
import ar.edu.um.turnos.catalog.support.TestEnvironment;
import ar.edu.um.turnos.catalog.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Evidence of the initialization from a snapshot (section 4.1 of the statement): a service
 * that boots against an empty database must fill its local copy on its own.
 *
 * <p>The class keeps its own container, its own context and its own gateway configuration,
 * which is what makes it independent from the rest of the suite: the initialization happens
 * while the context starts, before any test method runs, and the data it writes is committed
 * by the startup routine itself.</p>
 */
@Testcontainers
@SpringBootTest(classes = {CatalogApplication.class, PreloadedCatalogSnapshotConfiguration.class})
@AutoConfigureMockMvc
@Transactional
class CatalogStartupSyncTest {

    /**
     * Started for this class only: the snapshot it applies must not leak into the other
     * tests of the suite, which assume an empty local copy.
     */
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        TestEnvironment.register(registry, POSTGRES);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void bootsWithAnEmptyDatabaseAndAppliesThePublishedSnapshot() throws Exception {
        mockMvc.perform(get("/api/sync/status")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedVersion").value(7))
                .andExpect(jsonPath("$.snapshotVersion").value(7))
                .andExpect(jsonPath("$.lastError").doesNotExist())
                .andExpect(jsonPath("$.lastSyncAt").isNotEmpty());

        // The three collections are already readable through the API, without any forced sync.
        mockMvc.perform(get("/api/professionals")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"));

        mockMvc.perform(get("/api/professional-categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/professionals/" + CatalogFixtures.ANA_PEREZ)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weeklySchedules.length()").value(2));
    }

    private static String bearer() {
        return "Bearer " + TestTokens.hs256(TestEnvironment.TEST_JWT_SECRET, "test.user");
    }
}
