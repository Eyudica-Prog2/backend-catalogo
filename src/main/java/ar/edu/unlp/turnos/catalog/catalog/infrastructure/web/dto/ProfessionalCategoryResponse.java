package ar.edu.unlp.turnos.catalog.catalog.infrastructure.web.dto;

/**
 * Body of {@code GET /api/professional-categories}.
 *
 * @param id          id assigned by the catedra
 * @param name        display name
 * @param description optional description, omitted when absent
 * @param enabled     logical state
 */
public record ProfessionalCategoryResponse(Long id,
                                           String name,
                                           String description,
                                           boolean enabled) {
}
