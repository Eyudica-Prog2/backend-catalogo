package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto;

/**
 * Professional as returned by the public search and by the internal contract consumed by
 * {@code backend-turnos} ({@code GET /api/internal/professionals/{id}}): both expose exactly
 * the same five fields, so one DTO serves the two endpoints.
 *
 * @param id         id assigned by the catedra
 * @param categoryId category the professional belongs to
 * @param firstName  first name
 * @param lastName   last name
 * @param enabled    logical state
 */
public record ProfessionalResponse(Long id,
                                   Long categoryId,
                                   String firstName,
                                   String lastName,
                                   boolean enabled) {
}
