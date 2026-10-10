package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Weekly availability rule of a professional: one weekday, one time range and the duration
 * of each slot.
 *
 * <p>Pure domain model. The rules that make a schedule usable (a range that starts before it
 * ends and a positive slot duration) are checked by {@link CatalogSnapshot#validate()}, so a
 * payload that cannot be represented is rejected before it reaches the database.</p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class WeeklySchedule {

    private Long id;
    private Long professionalId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private int slotDurationMinutes;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
