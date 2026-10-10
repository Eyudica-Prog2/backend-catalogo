package ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto;

import java.util.List;

/**
 * Body of {@code GET /api/professionals/{id}}: the professional plus its weekly schedules.
 *
 * @param id               id assigned by the catedra
 * @param categoryId       category the professional belongs to
 * @param firstName        first name
 * @param lastName         last name
 * @param enabled          logical state
 * @param weeklySchedules  every weekly schedule of the professional, enabled or not
 */
public record ProfessionalDetailResponse(Long id,
                                         Long categoryId,
                                         String firstName,
                                         String lastName,
                                         boolean enabled,
                                         List<WeeklyScheduleResponse> weeklySchedules) {

    public ProfessionalDetailResponse {
        weeklySchedules = weeklySchedules == null ? List.of() : List.copyOf(weeklySchedules);
    }
}
