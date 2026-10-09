package ar.edu.unlp.turnos.catalog.catalog.domain.ports.out;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogChanges;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import java.util.Optional;

/**
 * Outbound port: read access to the incremental feed published by the catedra in Redis
 * (contract v1, section 14).
 *
 * <p>The port is deliberately synchronous and read only. Redis is <strong>not</strong> a
 * source of truth for the local copy: it only exposes the window, the ids affected by each
 * version and the current state of those ids. The decision of what to apply belongs to the
 * application layer, and the write belongs to {@link CatalogSyncRepository}.</p>
 *
 * <p>The implementation owns the connection (credentials, timeouts, namespaces) and the
 * parsing of the published JSON; the caller only sees domain types or an error with a
 * stable functional code.</p>
 */
public interface CatalogIncrementalGateway {

    /**
     * @return the published version window ({@code currentVersion},
     *         {@code oldestAvailableVersion} and publication instant)
     * @throws RuntimeException when Redis is unreachable or the published values do not
     *                          describe a usable window
     */
    CatalogVersionWindow fetchVersionWindow();

    /**
     * @param version version whose affected ids must be read
     * @return the published delta, empty when that version is no longer published (which
     *         means the local copy cannot be continued incrementally)
     * @throws RuntimeException when Redis is unreachable or the payload cannot be read
     */
    Optional<CatalogChanges> fetchChanges(long version);

    /**
     * @param categoryId remote id of the category
     * @return its current state in the published hash, empty when it is not published
     *         anymore (a category that disappeared cannot be applied blindly)
     */
    Optional<ProfessionalCategory> fetchCategory(long categoryId);

    /**
     * @param professionalId remote id of the professional
     * @return its current state in the published hash, empty when it is not published
     *         anymore
     */
    Optional<Professional> fetchProfessional(long professionalId);

    /**
     * @param scheduleId remote id of the weekly schedule
     * @return its current state in the published hash, empty when it is not published
     *         anymore
     */
    Optional<WeeklySchedule> fetchWeeklySchedule(long scheduleId);
}
