package ar.edu.unlp.turnos.catalog.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.edu.unlp.turnos.catalog.CatalogApplication;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.support.CatedraRedisFixture;
import ar.edu.unlp.turnos.catalog.support.RecordingCatalogIncrementalGateway;
import ar.edu.unlp.turnos.catalog.support.RecordingSyncConfiguration;
import ar.edu.unlp.turnos.catalog.support.StubCatalogSnapshotGateway;
import ar.edu.unlp.turnos.catalog.support.StubCatalogSnapshotConfiguration;
import ar.edu.unlp.turnos.catalog.support.TestEnvironment;
import ar.edu.unlp.turnos.catalog.support.TestTokens;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class of the integration tests of the catalog API: real Spring context, MockMvc and a
 * real PostgreSQL container (no H2 anywhere).
 *
 * <p>Decisions worth documenting:</p>
 * <ul>
 *   <li>the snapshot gateway is replaced by {@link StubCatalogSnapshotGateway}, so no test
 *       ever performs an HTTP call; the startup routine therefore finds an "unreachable
 *       catedra" and leaves the local copy empty, which is the state every test starts from;</li>
 *   <li>the container is started once and never stopped: every subclass shares one context
 *       and one database, and each test runs inside a transaction that is rolled back;</li>
 *   <li>the incremental feed is read through a real Redis container (see
 *       {@link TestEnvironment}), decorated by {@link RecordingCatalogIncrementalGateway}
 *       so a test can prove which key was read and which one was skipped;</li>
 *   <li>the catedra base url points to a closed port with one attempt, so a test that
 *       accidentally reaches the real client fails fast instead of hanging.</li>
 * </ul>
 */
@SpringBootTest(classes = {CatalogApplication.class, StubCatalogSnapshotConfiguration.class,
        RecordingSyncConfiguration.class})
@AutoConfigureMockMvc
@Transactional
public abstract class CatalogApiIntegrationTest {

    /**
     * Started on the first context creation and kept for the whole test run: a restarted
     * container would change its port while the cached context still points to the old one.
     */
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine").asCompatibleSubstituteFor("postgres"));

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        TestEnvironment.register(registry, POSTGRES);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected StubCatalogSnapshotGateway snapshotGateway;

    @Autowired
    protected RecordingCatalogIncrementalGateway incrementalGateway;

    @BeforeEach
    void resetTestInfrastructure() {
        snapshotGateway.reset();
        incrementalGateway.reset();
        // Redis has no transaction to roll back: the feed is emptied before each test so the
        // window and the diagnostic of one test can never decide the outcome of the next.
        CatedraRedisFixture.clear();
    }

    /**
     * @return the {@code Authorization} header of a valid end user
     */
    protected String bearer() {
        return "Bearer " + TestTokens.hs256(TestEnvironment.TEST_JWT_SECRET, "test.user");
    }

    /**
     * Makes the gateway answer with the given snapshot and runs
     * {@code POST /api/internal/sync/force}, so a test starts from a known local copy.
     *
     * @param snapshot snapshot the catedra must publish
     * @return the result of the forced synchronization
     * @throws Exception when MockMvc fails
     */
    protected ResultActions syncFrom(CatalogSnapshot snapshot) throws Exception {
        snapshotGateway.returns(snapshot);
        return forceSync();
    }

    /**
     * @return the result of {@code POST /api/internal/sync/force} with a valid token
     * @throws Exception when MockMvc fails
     */
    protected ResultActions forceSync() throws Exception {
        return mockMvc.perform(post("/api/internal/sync/force")
                .header(HttpHeaders.AUTHORIZATION, bearer()));
    }

    /**
     * Applies what the catedra published in Redis since the local copy, through
     * {@code POST /api/internal/sync/run} with a valid token.
     *
     * @return the result of the incremental synchronization
     * @throws Exception when MockMvc fails
     */
    protected ResultActions runSync() throws Exception {
        return mockMvc.perform(post("/api/internal/sync/run")
                .header(HttpHeaders.AUTHORIZATION, bearer()));
    }
}
