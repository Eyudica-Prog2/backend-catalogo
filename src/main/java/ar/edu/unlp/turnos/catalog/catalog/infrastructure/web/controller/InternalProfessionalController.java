package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.controller;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklyScheduleQuery;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.GetProfessionalByIdUseCase;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.in.GetWeeklySchedulesUseCase;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalResponse;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.WeeklyScheduleSlotResponse;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.mapper.ProfessionalDtoMapper;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal contract consumed by {@code backend-turnos} (section 4.2 of the statement: a
 * service must get the information it needs from the catalog service before starting an
 * operation).
 *
 * <p>These endpoints read the local copy only: they never call the catedra, which is what
 * keeps turnos independent from the availability of the catedra API. They sit behind the same
 * deny-by-default JWT filter as the rest of the API, so turnos must present a token issued for
 * this audience.</p>
 *
 * <p>The response shapes are the ones dictated by the iteration: a professional with
 * {@code id}, {@code categoryId}, {@code firstName}, {@code lastName} and {@code enabled}, and
 * weekly schedules with {@code startTime}, {@code endTime} and {@code slotDurationMinutes}.
 * The schedule list also carries {@code dayOfWeek}: without it the variant that is not
 * filtered by date could not be mapped to the dates of a range, which is the reason that
 * variant exists.</p>
 */
@RestController
@RequiredArgsConstructor
public class InternalProfessionalController {

    private final GetProfessionalByIdUseCase getProfessionalById;
    private final GetWeeklySchedulesUseCase getWeeklySchedules;
    private final ProfessionalDtoMapper mapper;

    /**
     * Reads one professional of the local copy.
     *
     * @param id remote id of the professional
     * @return 200 with the professional, 404 {@code PROFESSIONAL_NOT_FOUND} when the local
     *         copy does not contain it
     */
    @GetMapping("/api/internal/professionals/{id}")
    public ResponseEntity<ProfessionalResponse> getProfessional(@PathVariable("id") Long id) {
        ProfessionalDetails details = getProfessionalById.getProfessionalById(id);
        return ResponseEntity.ok(mapper.toResponse(details.professional()));
    }

    /**
     * Reads the weekly schedules of one professional.
     *
     * <p>Two shapes are served by the same endpoint with the same body:</p>
     * <ul>
     *   <li>{@code ?date=yyyy-MM-dd}: schedules applicable to that date, that is, the ones of
     *       its weekday; this is what turnos uses to build the availability of a day;</li>
     *   <li>without {@code date}: every enabled schedule, which turnos maps to the dates of
     *       a range through the {@code dayOfWeek} each slot carries.</li>
     * </ul>
     *
     * @param id   remote id of the professional
     * @param date optional date to filter by
     * @return 200 with the schedules ordered by weekday and start time, 404
     *         {@code PROFESSIONAL_NOT_FOUND} when the professional is unknown
     */
    @GetMapping("/api/internal/professionals/{id}/weekly-schedules")
    public ResponseEntity<List<WeeklyScheduleSlotResponse>> getWeeklySchedules(
            @PathVariable("id") Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        WeeklyScheduleQuery query = new WeeklyScheduleQuery(id, date, true);
        List<WeeklySchedule> schedules = getWeeklySchedules.getWeeklySchedules(query);
        return ResponseEntity.ok(mapper.toSlotResponses(schedules));
    }
}
