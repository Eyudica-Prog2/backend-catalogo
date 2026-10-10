package ar.edu.um.turnos.catalog.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.um.turnos.catalog.shared.error.ErrorCodes;
import ar.edu.um.turnos.catalog.support.CatalogFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/**
 * End to end tests of the public API over the local copy: the four mandatory filters, the
 * pagination headers and the detail endpoint (sections 6.1 and 6.2 of the statement).
 *
 * <p>Every test starts from the same local copy, applied with one forced synchronization.</p>
 */
class ProfessionalSearchApiTest extends CatalogApiIntegrationTest {

    @BeforeEach
    void applyLocalCopy() throws Exception {
        syncFrom(CatalogFixtures.snapshot(7)).andExpect(status().isOk());
    }

    @Test
    void searchWithoutTokenIsRejectedWith401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/professionals"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.UNAUTHORIZED));
    }

    @Test
    void searchAnswersTheFirstPageWithTotalCountAndLink() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(header().string(HttpHeaders.LINK, containsString("rel=\"first\"")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("rel=\"next\"")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("rel=\"last\"")))
                .andExpect(header().string(HttpHeaders.LINK, not(containsString("rel=\"prev\""))))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].lastName").value("Diaz"))
                .andExpect(jsonPath("$.[1].lastName").value("Gomez"));
    }

    @Test
    void searchKeepsTheOrderAcrossPages() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("page", "1")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "5"))
                .andExpect(header().string(HttpHeaders.LINK, containsString("rel=\"prev\"")))
                .andExpect(header().string(HttpHeaders.LINK, containsString("rel=\"next\"")))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].lastName").value("Perez"))
                .andExpect(jsonPath("$.[1].lastName").value("Ruiz"));

        mockMvc.perform(get("/api/professionals")
                        .param("page", "2")
                        .param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.LINK, containsString("rel=\"prev\"")))
                .andExpect(header().string(HttpHeaders.LINK, not(containsString("rel=\"next\""))))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].lastName").value("Sosa"));
    }

    @Test
    void searchSortsByLastNameDescendingWhenAsked() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("sort", "lastName,desc")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].lastName").value("Sosa"))
                .andExpect(jsonPath("$.[4].lastName").value("Diaz"));
    }

    @Test
    void searchFiltersByCategory() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("categoryId", String.valueOf(CatalogFixtures.HEALTH_CATEGORY))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.[0].lastName").value("Gomez"))
                .andExpect(jsonPath("$.[2].lastName").value("Ruiz"));
    }

    @Test
    void searchFiltersByNameOnFirstAndLastName() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("name", "ana")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.[0].firstName").value("Ana"))
                .andExpect(jsonPath("$.[0].lastName").value("Perez"))
                .andExpect(jsonPath("$.[1].lastName").value("Ruiz"));

        mockMvc.perform(get("/api/professionals")
                        .param("name", "SOS")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$.[0].lastName").value("Sosa"));
    }

    @Test
    void searchCombinesCategoryAndName() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("categoryId", String.valueOf(CatalogFixtures.HEALTH_CATEGORY))
                        .param("name", "ana")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void searchFiltersByEnabledState() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("enabled", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "4"));

        mockMvc.perform(get("/api/professionals")
                        .param("enabled", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "1"))
                .andExpect(jsonPath("$.[0].lastName").value("Diaz"))
                .andExpect(jsonPath("$.[0].enabled").value(false));
    }

    @Test
    void searchFiltersByAvailability() throws Exception {
        // available=true: professional with at least one enabled weekly schedule.
        mockMvc.perform(get("/api/professionals")
                        .param("available", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"));

        mockMvc.perform(get("/api/professionals")
                        .param("available", "false")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"));

        mockMvc.perform(get("/api/professionals")
                        .param("categoryId", String.valueOf(CatalogFixtures.HEALTH_CATEGORY))
                        .param("available", "true")
                        .param("enabled", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"));
    }

    @Test
    void searchWithoutMatchesAnswersAnEmptyPageWithTotalCountZero() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("name", "zzz")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void searchRejectsValuesThatCannotBeUsed() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("size", "0")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));

        mockMvc.perform(get("/api/professionals")
                        .param("size", "101")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));

        mockMvc.perform(get("/api/professionals")
                        .param("page", "-1")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));

        mockMvc.perform(get("/api/professionals")
                        .param("sort", "password,asc")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));

        mockMvc.perform(get("/api/professionals")
                        .param("sort", "lastName,sideways")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
    }

    @Test
    void searchRejectsParametersThatAreNotNumbers() throws Exception {
        mockMvc.perform(get("/api/professionals")
                        .param("page", "abc")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));

        mockMvc.perform(get("/api/professionals")
                        .param("categoryId", "not-an-id")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
    }

    @Test
    void categoriesEndpointReturnsEveryLocalCategory() throws Exception {
        mockMvc.perform(get("/api/professional-categories")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].name").value("Deporte"))
                .andExpect(jsonPath("$.[0].enabled").value(false))
                .andExpect(jsonPath("$.[0].description").doesNotExist())
                .andExpect(jsonPath("$.[1].name").value("Salud"))
                .andExpect(jsonPath("$.[1].enabled").value(true))
                .andExpect(jsonPath("$.[1].description").value("Profesionales de la salud"));
    }

    @Test
    void professionalDetailIncludesItsWeeklySchedules() throws Exception {
        mockMvc.perform(get("/api/professionals/" + CatalogFixtures.ANA_PEREZ)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$.id").value(CatalogFixtures.ANA_PEREZ))
                .andExpect(jsonPath("$.categoryId").value(CatalogFixtures.HEALTH_CATEGORY))
                .andExpect(jsonPath("$.firstName").value("Ana"))
                .andExpect(jsonPath("$.lastName").value("Perez"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.weeklySchedules.length()").value(2))
                .andExpect(jsonPath("$.weeklySchedules[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.weeklySchedules[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].endTime").value("13:00:00"))
                .andExpect(jsonPath("$.weeklySchedules[0].slotDurationMinutes").value(30))
                .andExpect(jsonPath("$.weeklySchedules[0].enabled").value(true))
                .andExpect(jsonPath("$.weeklySchedules[1].dayOfWeek").value("SATURDAY"))
                .andExpect(jsonPath("$.weeklySchedules[1].enabled").value(false));
    }

    @Test
    void professionalDetailOfAnUnknownIdIsA404WithItsFunctionalCode() throws Exception {
        mockMvc.perform(get("/api/professionals/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value(ErrorCodes.PROFESSIONAL_NOT_FOUND))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
