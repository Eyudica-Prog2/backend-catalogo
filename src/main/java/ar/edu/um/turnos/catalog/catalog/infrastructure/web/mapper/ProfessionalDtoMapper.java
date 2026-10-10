package ar.edu.um.turnos.catalog.catalog.infrastructure.web.mapper;

import ar.edu.um.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.um.turnos.catalog.catalog.domain.model.SortDirection;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalDetailResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.WeeklyScheduleResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.web.dto.WeeklyScheduleSlotResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Maps the domain of the local copy into the bodies of the public API and of the internal
 * contract, and reads the raw query parameters of the search into the domain criteria.
 *
 * <p>Parameter reading lives here (infrastructure) but the rules that decide which values are
 * valid live in {@link ProfessionalSearchCriteria}: this mapper only translates the rejection
 * into the {@code 400 VALIDATION_ERROR} that the HTTP contract exposes.</p>
 */
@Component
public class ProfessionalDtoMapper {

    /**
     * Reads {@code categoryId}, {@code name}, {@code enabled}, {@code available}, {@code page},
     * {@code size} and {@code sort} into one criteria.
     *
     * <p>{@code sort} accepts {@code property} or {@code property,asc|desc} and is limited to
     * the properties a client is allowed to order by.</p>
     *
     * @param categoryId category filter
     * @param name       partial name filter
     * @param enabled    enabled state filter
     * @param available  availability filter
     * @param page       zero based page index
     * @param size       page size
     * @param sort       raw sort parameter
     * @return the criteria to execute
     * @throws CatalogException {@code 400 VALIDATION_ERROR} when a value cannot be used
     */
    public ProfessionalSearchCriteria toCriteria(Long categoryId, String name, Boolean enabled,
                                                 Boolean available, int page, int size, String sort) {
        SortDirection direction = readDirection(sort);
        String property = readSortProperty(sort);
        try {
            return new ProfessionalSearchCriteria(categoryId, name, enabled, available,
                    page, size, property, direction);
        } catch (IllegalArgumentException rejected) {
            throw CatalogException.invalidRequest(rejected.getMessage());
        }
    }

    private static String readSortProperty(String sort) {
        if (sort == null || sort.isBlank()) {
            return null;
        }
        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw CatalogException.invalidRequest("sort must be <property> or <property>,asc|desc.");
        }
        return parts[0];
    }

    private static SortDirection readDirection(String sort) {
        if (sort == null || sort.isBlank()) {
            return SortDirection.ASC;
        }
        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw CatalogException.invalidRequest("sort must be <property> or <property>,asc|desc.");
        }
        if (parts.length == 1) {
            return SortDirection.ASC;
        }
        try {
            return SortDirection.from(parts[1]);
        } catch (IllegalArgumentException badDirection) {
            throw CatalogException.invalidRequest("sort direction must be asc or desc.");
        }
    }

    /**
     * @param categories categories of the local copy, may be null
     * @return one response per category, null entries are skipped
     */
    public List<ProfessionalCategoryResponse> toCategoryResponses(List<ProfessionalCategory> categories) {
        if (categories == null) {
            return List.of();
        }
        return categories.stream()
                .filter(Objects::nonNull)
                .map(this::toCategoryResponse)
                .toList();
    }

    /**
     * @param category category to map, may be null
     * @return the response, or null when the category is null
     */
    public ProfessionalCategoryResponse toCategoryResponse(ProfessionalCategory category) {
        if (category == null) {
            return null;
        }
        return new ProfessionalCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isEnabled());
    }

    /**
     * @param professionals professionals to map, may be null
     * @return one response per professional, null entries are skipped
     */
    public List<ProfessionalResponse> toResponses(List<Professional> professionals) {
        if (professionals == null) {
            return List.of();
        }
        return professionals.stream()
                .filter(Objects::nonNull)
                .map(this::toResponse)
                .toList();
    }

    /**
     * @param professional professional to map, may be null
     * @return the response, or null when the professional is null
     */
    public ProfessionalResponse toResponse(Professional professional) {
        if (professional == null) {
            return null;
        }
        return new ProfessionalResponse(
                professional.getId(),
                professional.getCategoryId(),
                professional.getFirstName(),
                professional.getLastName(),
                professional.isEnabled());
    }

    /**
     * @param details professional plus its schedules, may be null
     * @return the detail body, or null when the details are null
     */
    public ProfessionalDetailResponse toDetailResponse(ProfessionalDetails details) {
        if (details == null) {
            return null;
        }
        return new ProfessionalDetailResponse(
                details.professional().getId(),
                details.professional().getCategoryId(),
                details.professional().getFirstName(),
                details.professional().getLastName(),
                details.professional().isEnabled(),
                toScheduleResponses(details.weeklySchedules()));
    }

    /**
     * @param schedules schedules to map, may be null
     * @return one response per schedule, null entries are skipped
     */
    public List<WeeklyScheduleResponse> toScheduleResponses(List<WeeklySchedule> schedules) {
        if (schedules == null) {
            return List.of();
        }
        return schedules.stream()
                .filter(Objects::nonNull)
                .map(schedule -> new WeeklyScheduleResponse(
                        schedule.getId(),
                        schedule.getProfessionalId(),
                        schedule.getDayOfWeek(),
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        schedule.getSlotDurationMinutes(),
                        schedule.isEnabled()))
                .toList();
    }

    /**
     * Slots of the internal contract. Every slot repeats its weekday, which is what
     * {@code backend-turnos} uses to map a range of dates to the days it covers.
     *
     * @param schedules schedules to map, may be null
     * @return one slot per schedule, null entries are skipped
     */
    public List<WeeklyScheduleSlotResponse> toSlotResponses(List<WeeklySchedule> schedules) {
        if (schedules == null) {
            return List.of();
        }
        return schedules.stream()
                .filter(Objects::nonNull)
                .map(schedule -> new WeeklyScheduleSlotResponse(
                        schedule.getDayOfWeek(),
                        schedule.getStartTime(),
                        schedule.getEndTime(),
                        schedule.getSlotDurationMinutes()))
                .toList();
    }
}
