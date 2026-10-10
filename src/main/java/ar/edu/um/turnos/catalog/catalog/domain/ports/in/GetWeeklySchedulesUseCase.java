package ar.edu.um.turnos.catalog.catalog.domain.ports.in;

import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklyScheduleQuery;
import java.util.List;

/**
 * Use case: read the weekly schedules of a professional.
 *
 * <p>Two shapes are supported through the same port: the schedules applicable to a concrete
 * date (used by {@code backend-turnos} to build the availability of a day) and every enabled
 * schedule (used to build a date range).</p>
 */
public interface GetWeeklySchedulesUseCase {

    /**
     * @param query professional, optional date and enabled filter
     * @return the matching weekly schedules, ordered by weekday and start time
     * @throws RuntimeException {@code 404 PROFESSIONAL_NOT_FOUND} when the professional is
     *                          not part of the local copy
     */
    List<WeeklySchedule> getWeeklySchedules(WeeklyScheduleQuery query);
}
