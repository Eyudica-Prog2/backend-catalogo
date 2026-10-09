package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unlp.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalSearchCriteria;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.SortDirection;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalCategoryResponse;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalDetailResponse;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.ProfessionalResponse;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto.WeeklyScheduleSlotResponse;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import java.util.Arrays;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

/**
 * Unit test of the mapper between the domain of the local copy and the bodies of the API.
 *
 * <p>It covers both directions: reading query parameters (valid, blank and invalid values)
 * and building responses (including nulls and empty collections).</p>
 */
class ProfessionalDtoMapperTest {

    private final ProfessionalDtoMapper mapper = new ProfessionalDtoMapper();

    @Test
    void readsTheSortParameterIntoPropertyAndDirection() {
        ProfessionalSearchCriteria withDirection = mapper.toCriteria(
                10L, "ana", true, false, 1, 10, "firstName,desc");
        assertThat(withDirection.sortProperty()).isEqualTo("firstName");
        assertThat(withDirection.sortDirection()).isEqualTo(SortDirection.DESC);
        assertThat(withDirection.categoryId()).isEqualTo(10L);
        assertThat(withDirection.name()).isEqualTo("ana");
        assertThat(withDirection.enabled()).isTrue();
        assertThat(withDirection.available()).isFalse();
        assertThat(withDirection.page()).isEqualTo(1);
        assertThat(withDirection.size()).isEqualTo(10);

        ProfessionalSearchCriteria propertyOnly = mapper.toCriteria(null, null, null, null, 0, 20, "id");
        assertThat(propertyOnly.sortProperty()).isEqualTo("id");
        assertThat(propertyOnly.sortDirection()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void acceptsABlankSortParameterAsTheDefaultOrder() {
        assertThat(mapper.toCriteria(null, null, null, null, 0, 20, "").sortProperty())
                .isEqualTo("lastName");
        assertThat(mapper.toCriteria(null, null, null, null, 0, 20, null).sortProperty())
                .isEqualTo("lastName");
    }

    @Test
    void translatesEveryRejectedParameterIntoAValidationProblem() {
        assertRejected(() -> mapper.toCriteria(null, null, null, null, -1, 20, null));
        assertRejected(() -> mapper.toCriteria(null, null, null, null, 0, 0, null));
        assertRejected(() -> mapper.toCriteria(null, null, null, null, 0, 101, null));
        assertRejected(() -> mapper.toCriteria(null, null, null, null, 0, 20, "password"));
        assertRejected(() -> mapper.toCriteria(null, null, null, null, 0, 20, "lastName,sideways"));
        assertRejected(() -> mapper.toCriteria(null, null, null, null, 0, 20, "id,asc,extra"));
    }

    @Test
    void buildsResponsesAndSkipsWhatIsNotThere() {
        assertThat(mapper.toResponses(null)).isEmpty();
        assertThat(mapper.toCategoryResponses(null)).isEmpty();
        assertThat(mapper.toSlotResponses(null)).isEmpty();
        assertThat(mapper.toResponse(null)).isNull();
        assertThat(mapper.toCategoryResponse(null)).isNull();
        assertThat(mapper.toDetailResponse(null)).isNull();

        Professional professional = CatalogFixtures.snapshot(1).getProfessionals().get(0);
        assertThat(mapper.toResponses(Arrays.asList(professional, null)))
                .containsExactly(toResponse(professional));
    }

    @Test
    void buildsTheDetailWithEveryScheduleOfTheProfessional() {
        CatalogSnapshot snapshot = CatalogFixtures.snapshot(1);
        Professional professional = snapshot.getProfessionals().get(0);
        List<WeeklySchedule> schedules = snapshot.getWeeklySchedules().stream()
                .filter(schedule -> schedule.getProfessionalId().equals(professional.getId()))
                .toList();

        ProfessionalDetailResponse response =
                mapper.toDetailResponse(new ProfessionalDetails(professional, schedules));

        assertThat(response.id()).isEqualTo(professional.getId());
        assertThat(response.categoryId()).isEqualTo(professional.getCategoryId());
        assertThat(response.firstName()).isEqualTo(professional.getFirstName());
        assertThat(response.lastName()).isEqualTo(professional.getLastName());
        assertThat(response.enabled()).isEqualTo(professional.isEnabled());
        assertThat(response.weeklySchedules()).hasSize(schedules.size());
        assertThat(response.weeklySchedules().get(0).dayOfWeek()).isEqualTo(schedules.get(0).getDayOfWeek());
        assertThat(response.weeklySchedules().get(0).slotDurationMinutes())
                .isEqualTo(schedules.get(0).getSlotDurationMinutes());
    }

    @Test
    void mapsEverySlotOfTheInternalContract() {
        List<WeeklySchedule> schedules =
                List.of(CatalogFixtures.snapshot(1).getWeeklySchedules().get(0));

        List<WeeklyScheduleSlotResponse> slots = mapper.toSlotResponses(schedules);

        assertThat(slots).hasSize(1);
        assertThat(slots.get(0).dayOfWeek()).isEqualTo(schedules.get(0).getDayOfWeek());
        assertThat(slots.get(0).startTime()).isEqualTo(schedules.get(0).getStartTime());
        assertThat(slots.get(0).endTime()).isEqualTo(schedules.get(0).getEndTime());
        assertThat(slots.get(0).slotDurationMinutes())
                .isEqualTo(schedules.get(0).getSlotDurationMinutes());
    }

    @Test
    void mapsCategoriesKeepingTheirLogicalState() {
        ProfessionalCategory category = CatalogFixtures.snapshot(1).getProfessionalCategories().get(1);

        ProfessionalCategoryResponse response = mapper.toCategoryResponse(category);

        assertThat(response.id()).isEqualTo(category.getId());
        assertThat(response.name()).isEqualTo(category.getName());
        assertThat(response.description()).isEqualTo(category.getDescription());
        assertThat(response.enabled()).isEqualTo(category.isEnabled());
    }

    private static ProfessionalResponse toResponse(Professional professional) {
        return new ProfessionalResponse(professional.getId(), professional.getCategoryId(),
                professional.getFirstName(), professional.getLastName(), professional.isEnabled());
    }

    private static void assertRejected(ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(CatalogException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception.getHttpStatus()).isEqualTo(400);
                    assertThat(exception.getDetail()).isNotBlank();
                });
    }
}
