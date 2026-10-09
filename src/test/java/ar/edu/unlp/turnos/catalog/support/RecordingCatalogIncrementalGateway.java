package ar.edu.unlp.turnos.catalog.support;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogChanges;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogIncrementalGateway;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Decorates the real incremental gateway so a test can see <em>which</em> key of the catedra
 * was read and when.
 *
 * <p>It is not a fake: every call is delegated to {@code RedisCatalogIncrementalGateway},
 * which reads a real Redis container. Only the trace is added, because "the local copy was
 * rebuilt from a snapshot" and "the missing delta was never requested" are different claims,
 * and only the second one proves the fallback was taken at the right moment.</p>
 */
public class RecordingCatalogIncrementalGateway implements CatalogIncrementalGateway {

    private final CatalogIncrementalGateway delegate;
    private final List<String> operations = Collections.synchronizedList(new ArrayList<>());

    /**
     * @param delegate real adapter that performs the reads
     */
    public RecordingCatalogIncrementalGateway(CatalogIncrementalGateway delegate) {
        this.delegate = delegate;
    }

    @Override
    public CatalogVersionWindow fetchVersionWindow() {
        return record("fetchVersionWindow", delegate::fetchVersionWindow);
    }

    @Override
    public Optional<CatalogChanges> fetchChanges(long version) {
        return record("fetchChanges:" + version, () -> delegate.fetchChanges(version));
    }

    @Override
    public Optional<ProfessionalCategory> fetchCategory(long categoryId) {
        return record("fetchCategory:" + categoryId, () -> delegate.fetchCategory(categoryId));
    }

    @Override
    public Optional<Professional> fetchProfessional(long professionalId) {
        return record("fetchProfessional:" + professionalId, () -> delegate.fetchProfessional(professionalId));
    }

    @Override
    public Optional<WeeklySchedule> fetchWeeklySchedule(long scheduleId) {
        return record("fetchWeeklySchedule:" + scheduleId, () -> delegate.fetchWeeklySchedule(scheduleId));
    }

    /**
     * Forgets the trace, so the operations of one test never appear in the next one.
     */
    public void reset() {
        operations.clear();
    }

    /**
     * @return every operation performed since the last {@link #reset()}, in order
     */
    public List<String> operations() {
        synchronized (operations) {
            return List.copyOf(operations);
        }
    }

    /**
     * @param version version whose delta must have been requested
     * @return true when the delta of that version was read
     */
    public boolean changesRequestedFor(long version) {
        return operations().contains("fetchChanges:" + version);
    }

    /**
     * @return the versions whose delta was read, in order
     */
    public List<Long> changesVersionsRequested() {
        return operations().stream()
                .filter(operation -> operation.startsWith("fetchChanges:"))
                .map(operation -> Long.parseLong(operation.substring("fetchChanges:".length())))
                .toList();
    }

    private <T> T record(String operation, java.util.function.Supplier<T> call) {
        operations.add(operation);
        return call.get();
    }
}
