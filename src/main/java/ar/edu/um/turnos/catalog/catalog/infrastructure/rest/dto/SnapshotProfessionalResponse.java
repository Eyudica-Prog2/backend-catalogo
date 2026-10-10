package ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto;

import java.time.Instant;

/**
 * Professional inside the catalog snapshot.
 *
 * @param id          id assigned by the catedra
 * @param categoryId  category the professional belongs to
 * @param firstName   first name
 * @param lastName    last name
 * @param enabled     logical state; {@code null} means the field was not sent
 * @param createdAt   instant of creation reported by the catedra
 * @param updatedAt   instant of the last change reported by the catedra
 */
public record SnapshotProfessionalResponse(Long id,
                                           Long categoryId,
                                           String firstName,
                                           String lastName,
                                           Boolean enabled,
                                           Instant createdAt,
                                           Instant updatedAt) {
}
