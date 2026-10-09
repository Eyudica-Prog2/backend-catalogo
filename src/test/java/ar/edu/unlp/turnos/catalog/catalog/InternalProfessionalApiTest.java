package ar.edu.unlp.turnos.catalog.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unlp.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * End to end tests of the internal contract consumed by {@code backend-turnos} (section 5 of
 * the statement): professional, schedules of a date and schedules of a range.
 */
class InternalProfessionalApiTest extends CatalogApiIntegrationTest {

    /** Any Monday and any Saturday: the weekday is what the filter really uses. */
    private static final LocalDate ANY_MONDAY =
            LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    private static final LocalDate ANY_SATURDAY =
            LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

    @BeforeEach
    void applyLocalCopy() throws Exception {
        syncFrom(CatalogFixtures.snapshot(7)).andExpect(status().isOk());
    }

    @Test
    void internalProfessionalWithoutTokenIsRejectedWith401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.ANA_PEREZ))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void internalProfessionalExposesExactlyTheContractFields() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.ANA_PEREZ)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$.id").value(CatalogFixtures.ANA_PEREZ))
                .andExpect(jsonPath("$.categoryId").value(CatalogFixtures.HEALTH_CATEGORY))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Perez"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void internalProfessionalOfAnUnknownIdIsA404WithItsFunctionalCode() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCodes.PROFESSIONAL_NOT_FOUND))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void weeklySchedulesFilteredByDateOnlyReturnTheSchedulesOfThatWeekday() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.ANA_PEREZ + "/weekly-schedules")
                        .param("date", ANY_MONDAY.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.[0].endTime").value("13:00:00"))
                .andExpect(jsonPath("$.[0].slotDurationMinutes").value(30));

        // Saturday of the same professional is disabled, so the day has no availability.
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.ANA_PEREZ + "/weekly-schedules")
                        .param("date", ANY_SATURDAY.toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void weeklySchedulesWithoutDateReturnEveryEnabledSchedule() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.DIEGO_SOSA + "/weekly-schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].length()").value(4))
                .andExpect(jsonPath("$.[0].dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.[0].endTime").value("11:00:00"))
                .andExpect(jsonPath("$.[0].slotDurationMinutes").value(60))
                .andExpect(jsonPath("$.[1].dayOfWeek").value("THURSDAY"));

        // A professional whose only schedule is disabled answers an empty list, not an error.
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.BRUNO_GOMEZ + "/weekly-schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void weeklySchedulesOfAnUnknownProfessionalIsA404() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/999999/weekly-schedules")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.PROFESSIONAL_NOT_FOUND));
    }

    @Test
    void weeklySchedulesRejectADateThatIsNotADate() throws Exception {
        mockMvc.perform(get("/api/internal/professionals/" + CatalogFixtures.ANA_PEREZ + "/weekly-schedules")
                        .param("date", "12-10-2026")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
    }
}
