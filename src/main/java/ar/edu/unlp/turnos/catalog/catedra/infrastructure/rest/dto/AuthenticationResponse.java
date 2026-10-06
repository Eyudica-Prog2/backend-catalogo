package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response of {@code POST /api/authenticate} and starting point of the registration flow.
 *
 * @param idToken     technical JWT of this project, valid for one year; it never reaches KMP
 * @param integration current integration object
 */
public record AuthenticationResponse(
        @JsonProperty("id_token") String idToken,
        IntegrationResponse integration) {
}
