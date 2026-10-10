package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.util.List;

/**
 * Professional with the weekly schedules stored in the local copy.
 *
 * <p>Returned by {@code GET /api/professionals/{id}} (public API) and by the internal
 * contract consumed by {@code backend-turnos}.</p>
 *
 * @param professional    the professional itself
 * @param weeklySchedules every weekly schedule of the professional, enabled or not
 */
public record ProfessionalDetails(Professional professional, List<WeeklySchedule> weeklySchedules) {

    public ProfessionalDetails {
        weeklySchedules = weeklySchedules == null ? List.of() : List.copyOf(weeklySchedules);
    }
}
