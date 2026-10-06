package ar.edu.unlp.turnos.catalog.catedra.infrastructure.rest.dto;

/**
 * Body of {@code POST /api/authenticate} against the catedra (contract v1, section 5.2).
 *
 * @param username   technical login
 * @param password   technical password
 * @param rememberMe always true: asks for the long lived token (one year)
 */
public record AuthenticationRequest(String username, String password, boolean rememberMe) {
}
