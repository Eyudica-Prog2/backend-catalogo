package ar.edu.unlp.turnos.catalog.catalog.application.usecases;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklyScheduleQuery;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.GetProfessionalByIdUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.GetWeeklySchedulesUseCase;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reads the weekly schedules applicable to a query.
 *
 * <p>Rules applied here, next to the business they describe:</p>
 * <ul>
 *   <li>an unknown professional answers {@code 404 PROFESSIONAL_NOT_FOUND} instead of an
 *       empty list, which would look like a professional without agenda;</li>
 *   <li>when a date is given, only the schedules of that weekday apply to it;</li>
 *   <li>disabled schedules are excluded when the caller asks for enabled ones only;</li>
 *   <li>the result is ordered by weekday and start time, so both services see the same
 *       sequence.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class GetWeeklySchedulesUseCaseImpl implements GetWeeklySchedulesUseCase {

    private static final Comparator<WeeklySchedule> BY_DAY_AND_TIME =
            Comparator.comparing(WeeklySchedule::getDayOfWeek)
                    .thenComparing(WeeklySchedule::getStartTime);

    private final GetProfessionalByIdUseCase getProfessionalById;

    @Override
    public List<WeeklySchedule> getWeeklySchedules(WeeklyScheduleQuery query) {
        ProfessionalDetails details = getProfessionalById.getProfessionalById(query.professionalId());
        return details.weeklySchedules().stream()
                .filter(schedule -> !query.onlyEnabled() || schedule.isEnabled())
                .filter(schedule -> query.date() == null
                        || schedule.getDayOfWeek() == query.date().getDayOfWeek())
                .sorted(BY_DAY_AND_TIME)
                .toList();
    }
}
