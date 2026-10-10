package ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto;

import java.time.Instant;

/**
 * Current state published for one professional in the hash
 * {@code catedra:sync:professionals} (contract v1, section 14.4).
 *
 * @param id          id assigned by the catedra; {@code null} when the payload omits it
 * @param categoryId  category the professional belongs to
 * @param firstName   first name
 * @param lastName    last name
 * @param enabled     logical state; {@code null} means the field was not sent
 * @param createdAt   instant of creation reported by the catedra
 * @param updatedAt   instant of the last change reported by the catedra
 */
public record RedisProfessionalValue(Long id,
                                     Long categoryId,
                                     String firstName,
                                     String lastName,
                                     Boolean enabled,
                                     Instant createdAt,
                                     Instant updatedAt) {
}
