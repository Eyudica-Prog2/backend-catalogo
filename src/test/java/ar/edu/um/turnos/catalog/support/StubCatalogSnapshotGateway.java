package ar.edu.um.turnos.catalog.support;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogSnapshotGateway;
import ar.edu.um.turnos.catalog.shared.error.CatedraException;

/**
 * Snapshot gateway for tests.
 *
 * <p>It replaces the real adapter (which would perform an HTTP call against the catedra) and
 * lets each test decide what the catedra answers: a valid snapshot, an unreachable service or
 * nothing at all. Left unconfigured it behaves like an unreachable catedra, which is what the
 * startup routine sees during the tests: the local copy therefore starts empty on purpose.</p>
 */
public class StubCatalogSnapshotGateway implements CatalogSnapshotGateway {

    private CatalogSnapshot snapshot;
    private RuntimeException failure;
    private int fetchCount;

    /**
     * @param snapshot snapshot the gateway must answer with
     */
    public void returns(CatalogSnapshot snapshot) {
        this.snapshot = snapshot;
        this.failure = null;
    }

    /**
     * @param failure error the gateway must throw
     */
    public void fails(RuntimeException failure) {
        this.failure = failure;
        this.snapshot = null;
    }

    /**
     * Back to the default behaviour: unreachable catedra, no snapshot configured.
     */
    public void reset() {
        this.snapshot = null;
        this.failure = null;
        this.fetchCount = 0;
    }

    /**
     * @return how many times the gateway was asked for a snapshot
     */
    public int fetchCount() {
        return fetchCount;
    }

    @Override
    public CatalogSnapshot fetchCatalogSnapshot() {
        fetchCount++;
        if (failure != null) {
            throw failure;
        }
        if (snapshot == null) {
            throw CatedraException.unavailable("The catedra API is not available.");
        }
        return snapshot;
    }
}
