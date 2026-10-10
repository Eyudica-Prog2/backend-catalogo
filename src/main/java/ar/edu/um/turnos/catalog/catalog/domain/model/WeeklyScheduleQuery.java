package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.time.LocalDate;

/**
 * Criteria of {@code GET /api/internal/professionals/{id}/weekly-schedules}.
 *
 * @param professionalId professional whose schedules are requested
 * @param date           optional date: when present, only the schedules of that
 *                       {@code dayOfWeek} are returned (the ones applicable to the date)
 * @param onlyEnabled    when true, disabled schedules are excluded
 */
public record WeeklyScheduleQuery(Long professionalId, LocalDate date, boolean onlyEnabled) {

    public WeeklyScheduleQuery {
        if (professionalId == null) {
            throw new IllegalArgumentException("professionalId must not be null.");
        }
    }
}
