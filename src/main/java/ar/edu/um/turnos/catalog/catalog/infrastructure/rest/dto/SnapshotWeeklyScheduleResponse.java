package ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Weekly schedule inside the catalog snapshot.
 *
 * <p>{@code startTime} and {@code endTime} accept both {@code HH:mm} and {@code HH:mm:ss},
 * which is what the contract requires from every client (section 4).</p>
 *
 * @param id                   id assigned by the catedra
 * @param professionalId       professional the schedule belongs to
 * @param dayOfWeek            {@code MONDAY}..{@code SUNDAY}
 * @param startTime            local start time of the range
 * @param endTime              local end time of the range
 * @param slotDurationMinutes  duration of each slot; {@code null} means not sent
 * @param enabled              logical state; {@code null} means the field was not sent
 * @param createdAt            instant of creation reported by the catedra
 * @param updatedAt            instant of the last change reported by the catedra
 */
public record SnapshotWeeklyScheduleResponse(Long id,
                                             Long professionalId,
                                             DayOfWeek dayOfWeek,
                                             LocalTime startTime,
                                             LocalTime endTime,
                                             Integer slotDurationMinutes,
                                             Boolean enabled,
                                             Instant createdAt,
                                             Instant updatedAt) {
}
