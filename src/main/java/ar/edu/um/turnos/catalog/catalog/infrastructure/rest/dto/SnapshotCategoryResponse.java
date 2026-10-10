package ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto;

import java.time.Instant;

/**
 * Professional category inside the catalog snapshot.
 *
 * @param id          id assigned by the catedra
 * @param name        display name
 * @param description optional description
 * @param enabled     logical state; {@code null} means the field was not sent
 * @param createdAt   instant of creation reported by the catedra
 * @param updatedAt   instant of the last change reported by the catedra
 */
public record SnapshotCategoryResponse(Long id,
                                       String name,
                                       String description,
                                       Boolean enabled,
                                       Instant createdAt,
                                       Instant updatedAt) {
}
