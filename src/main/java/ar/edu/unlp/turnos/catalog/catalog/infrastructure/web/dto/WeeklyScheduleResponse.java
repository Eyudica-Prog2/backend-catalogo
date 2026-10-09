package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Weekly schedule as returned by {@code GET /api/professionals/{id}}.
 *
 * @param id                  id assigned by the catedra
 * @param professionalId      professional the schedule belongs to
 * @param dayOfWeek           {@code MONDAY}..{@code SUNDAY}
 * @param startTime           local start time, {@code HH:mm:ss}
 * @param endTime             local end time, {@code HH:mm:ss}
 * @param slotDurationMinutes duration of each slot
 * @param enabled             logical state; disabled schedules are shown here so the client
 *                            can explain why a weekday has no availability
 */
public record WeeklyScheduleResponse(Long id,
                                     Long professionalId,
                                     DayOfWeek dayOfWeek,
                                     LocalTime startTime,
                                     LocalTime endTime,
                                     int slotDurationMinutes,
                                     boolean enabled) {
}
