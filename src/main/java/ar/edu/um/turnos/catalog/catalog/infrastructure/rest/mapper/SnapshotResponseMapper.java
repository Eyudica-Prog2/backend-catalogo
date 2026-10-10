package ar.edu.um.turnos.catalog.catalog.infrastructure.rest.mapper;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotCategoryResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotProfessionalResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotWeeklyScheduleResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Maps the snapshot of contract v1 into the domain model of the local copy.
 *
 * <p>Structural omissions (a null collection, a missing version, a missing id) are mapped as
 * they arrive and rejected later by {@link CatalogSnapshot#validate()}, so a single set of
 * rules describes what a valid snapshot is. The only thing this mapper refuses to do is to
 * invent a value that the domain cannot represent: {@code enabled} is mandatory and a payload
 * without it is reported as {@code 503 SNAPSHOT_INVALID} instead of being defaulted.</p>
 */
@Component
public class SnapshotResponseMapper {

    /**
     * @param response payload received from the catedra, never null
     * @return the equivalent domain snapshot
     * @throws IllegalArgumentException when a required field cannot be represented
     */
    public CatalogSnapshot toDomain(SnapshotResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("The snapshot payload is empty.");
        }
        return CatalogSnapshot.builder()
                .snapshotVersion(response.snapshotVersion() == null ? 0L : response.snapshotVersion())
                .generatedAt(response.generatedAt())
                .professionalCategories(toCategories(response.professionalCategories()))
                .professionals(toProfessionals(response.professionals()))
                .weeklySchedules(toSchedules(response.weeklySchedules()))
                .build();
    }

    private List<ProfessionalCategory> toCategories(List<SnapshotCategoryResponse> categories) {
        if (categories == null) {
            return null;
        }
        return categories.stream().map(category -> {
            requireEnabled(category.enabled(), "A professional category of the snapshot");
            return ProfessionalCategory.builder()
                    .id(category.id())
                    .name(category.name())
                    .description(category.description())
                    .enabled(category.enabled())
                    .createdAt(category.createdAt())
                    .updatedAt(category.updatedAt())
                    .build();
        }).toList();
    }

    private List<Professional> toProfessionals(List<SnapshotProfessionalResponse> professionals) {
        if (professionals == null) {
            return null;
        }
        return professionals.stream().map(professional -> {
            requireEnabled(professional.enabled(), "A professional of the snapshot");
            return Professional.builder()
                    .id(professional.id())
                    .categoryId(professional.categoryId())
                    .firstName(professional.firstName())
                    .lastName(professional.lastName())
                    .enabled(professional.enabled())
                    .createdAt(professional.createdAt())
                    .updatedAt(professional.updatedAt())
                    .build();
        }).toList();
    }

    private List<WeeklySchedule> toSchedules(List<SnapshotWeeklyScheduleResponse> schedules) {
        if (schedules == null) {
            return null;
        }
        return schedules.stream().map(schedule -> {
            requireEnabled(schedule.enabled(), "A weekly schedule of the snapshot");
            return WeeklySchedule.builder()
                    .id(schedule.id())
                    .professionalId(schedule.professionalId())
                    .dayOfWeek(schedule.dayOfWeek())
                    .startTime(schedule.startTime())
                    .endTime(schedule.endTime())
                    .slotDurationMinutes(schedule.slotDurationMinutes() == null
                            ? 0
                            : schedule.slotDurationMinutes())
                    .enabled(schedule.enabled())
                    .createdAt(schedule.createdAt())
                    .updatedAt(schedule.updatedAt())
                    .build();
        }).toList();
    }

    private static void requireEnabled(Boolean enabled, String subject) {
        if (enabled == null) {
            throw new IllegalArgumentException(subject + " does not declare the required field enabled.");
        }
    }
}
