package ar.edu.um.turnos.catalog.catalog.application.usecases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import ar.edu.um.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklyScheduleQuery;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.GetProfessionalByIdUseCase;
import ar.edu.um.turnos.catalog.support.CatalogFixtures;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests of the use case that reads the weekly schedules of a professional.
 *
 * <p>Rules under test: a schedule only applies to the dates of its weekday, disabled
 * schedules are excluded when the caller asks for enabled ones, and the answer is ordered by
 * weekday and start time.</p>
 */
@ExtendWith(MockitoExtension.class)
class GetWeeklySchedulesUseCaseImplTest {

    private static final LocalDate ANY_MONDAY =
            LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    private static final LocalDate ANY_SATURDAY =
            LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

    @Mock
    private GetProfessionalByIdUseCase getProfessionalById;

    @InjectMocks
    private GetWeeklySchedulesUseCaseImpl useCase;

    @Test
    void returnsOnlyTheSchedulesOfTheRequestedWeekday() {
        givenLocalCopy();

        List<WeeklySchedule> schedules = useCase.getWeeklySchedules(
                new WeeklyScheduleQuery(CatalogFixtures.ANA_PEREZ, ANY_MONDAY, true));

        assertThat(schedules)
                .extracting(WeeklySchedule::getDayOfWeek)
                .containsExactly(DayOfWeek.MONDAY);
    }

    @Test
    void answersAnEmptyListWhenTheOnlyScheduleOfThatDayIsDisabled() {
        givenLocalCopy();

        List<WeeklySchedule> schedules = useCase.getWeeklySchedules(
                new WeeklyScheduleQuery(CatalogFixtures.ANA_PEREZ, ANY_SATURDAY, true));

        assertThat(schedules).isEmpty();
    }

    @Test
    void keepsDisabledSchedulesWhenTheCallerAsksForAllOfThem() {
        givenLocalCopy();

        List<WeeklySchedule> schedules = useCase.getWeeklySchedules(
                new WeeklyScheduleQuery(CatalogFixtures.ANA_PEREZ, null, false));

        assertThat(schedules)
                .extracting(WeeklySchedule::getDayOfWeek, WeeklySchedule::isEnabled)
                .containsExactly(
                        tuple(DayOfWeek.MONDAY, true),
                        tuple(DayOfWeek.SATURDAY, false));
    }

    @Test
    void answersNotFoundWhenTheProfessionalIsNotPartOfTheLocalCopy() {
        CatalogException notFound = CatalogException.professionalNotFound();
        when(getProfessionalById.getProfessionalById(999999L)).thenThrow(notFound);

        assertThatThrownBy(() -> useCase.getWeeklySchedules(
                        new WeeklyScheduleQuery(999999L, ANY_MONDAY, true)))
                .isSameAs(notFound);
    }

    private void givenLocalCopy() {
        Professional professional = Professional.builder()
                .id(CatalogFixtures.ANA_PEREZ)
                .categoryId(CatalogFixtures.HEALTH_CATEGORY)
                .firstName("Ana")
                .lastName("Perez")
                .enabled(true)
                .build();
        List<WeeklySchedule> schedules = CatalogFixtures.snapshot(1).getWeeklySchedules().stream()
                .filter(schedule -> schedule.getProfessionalId().equals(CatalogFixtures.ANA_PEREZ))
                .toList();
        when(getProfessionalById.getProfessionalById(CatalogFixtures.ANA_PEREZ))
                .thenReturn(new ProfessionalDetails(professional, schedules));
    }
}
