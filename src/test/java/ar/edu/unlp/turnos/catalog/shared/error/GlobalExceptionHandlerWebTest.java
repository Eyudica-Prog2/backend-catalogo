package ar.edu.unlp.turnos.catalog.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.edu.unlp.turnos.catalog.shared.error.web.ValidationFixtureController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web test of the single {@code @RestControllerAdvice}: status codes, functional codes,
 * content type and {@code fieldErrors}, with the security filters removed so the assertions
 * focus on the error contract.
 */
@WebMvcTest(ValidationFixtureController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void validationFailureAnswers400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validation/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR))
                .andExpect(jsonPath("$.message").value("error.validation"))
                .andExpect(jsonPath("$.path").value("/test/validation/echo"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].objectName").value("echoRequest"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    void malformedBodyAnswers400WithValidationCode() throws Exception {
        mockMvc.perform(post("/test/validation/echo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("this-is-not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_ERROR));
    }

    @Test
    void unknownEndpointAnswers404WithStableCode() throws Exception {
        mockMvc.perform(get("/test/validation/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.ENDPOINT_NOT_FOUND));
    }

    @Test
    void catedraAuthFailureKeepsItsOwnCodeAndStatus() throws Exception {
        mockMvc.perform(post("/test/validation/catedra-auth-failed"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(ErrorCodes.CATEDRA_AUTH_FAILED))
                .andExpect(jsonPath("$.message").value("error.http.503"));
    }

    @Test
    void catedraIntegrationNotFoundAnswers404() throws Exception {
        mockMvc.perform(post("/test/validation/catedra-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value(ErrorCodes.CATEDRA_INTEGRATION_NOT_FOUND));
    }
}
