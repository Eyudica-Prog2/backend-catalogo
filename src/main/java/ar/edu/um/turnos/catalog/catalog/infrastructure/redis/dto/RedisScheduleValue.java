package ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Current state published for one weekly schedule in the hash
 * {@code catedra:sync:weekly-schedules} (contract v1, section 14.4).
 *
 * @param id                   id assigned by the catedra; {@code null} when the payload omits it
 * @param professionalId       professional the schedule belongs to
 * @param dayOfWeek            {@code MONDAY}..{@code SUNDAY}
 * @param startTime            start time of the range
 * @param endTime              end time of the range
 * @param slotDurationMinutes  duration of each slot; {@code null} means not sent
 * @param enabled              logical state; {@code null} means the field was not sent
 * @param createdAt            instant of creation reported by the catedra
 * @param updatedAt            instant of the last change reported by the catedra
 */
public record RedisScheduleValue(Long id,
                                 Long professionalId,
                                 DayOfWeek dayOfWeek,
                                 LocalTime startTime,
                                 LocalTime endTime,
                                 Integer slotDurationMinutes,
                                 Boolean enabled,
                                 Instant createdAt,
                                 Instant updatedAt) {
}
