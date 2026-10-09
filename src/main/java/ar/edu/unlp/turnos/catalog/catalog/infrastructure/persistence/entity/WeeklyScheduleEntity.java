package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA representation of the {@code weekly_schedule} table: one weekday, one time range and
 * the duration of each slot for one professional.
 *
 * <p>{@code dayOfWeek} is stored as text ({@code MONDAY}..{@code SUNDAY}) so the values in
 * the database match the ones of the catedra contract and can be read with plain SQL.</p>
 */
@Entity
@Table(name = "weekly_schedule")
@Getter
@Setter
public class WeeklyScheduleEntity {

    @Id
    private Long id;

    @Column(name = "professional_id", nullable = false)
    private Long professionalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
