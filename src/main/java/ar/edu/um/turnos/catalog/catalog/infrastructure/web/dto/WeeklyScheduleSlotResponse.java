package ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Weekly schedule slot of the internal contract consumed by {@code backend-turnos}:
 * {@code GET /api/internal/professionals/{id}/weekly-schedules}.
 *
 * <p>The three fields required by the iteration ({@code startTime}, {@code endTime} and
 * {@code slotDurationMinutes}) plus {@code dayOfWeek}, which the variant without {@code date}
 * needs: to build the availability of a date range the caller must know which weekday each
 * slot repeats on. The shape is the same with and without the {@code date} filter.</p>
 *
 * @param dayOfWeek            weekday the schedule applies to
 * @param startTime            local start time, {@code HH:mm:ss}
 * @param endTime              local end time, {@code HH:mm:ss}
 * @param slotDurationMinutes  duration of each slot
 */
public record WeeklyScheduleSlotResponse(DayOfWeek dayOfWeek,
                                         LocalTime startTime,
                                         LocalTime endTime,
                                         int slotDurationMinutes) {
}
