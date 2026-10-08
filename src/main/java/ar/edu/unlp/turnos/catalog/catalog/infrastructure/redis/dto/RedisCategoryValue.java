package ar.edu.unlp.turnos.catalog.catalog.infrastructure.redis.dto;

import java.time.Instant;

/**
 * Current state published for one professional category in the hash
 * {@code catedra:sync:professional-categories} (contract v1, section 14.4).
 *
 * <p>The field of the hash is the decimal id and the value is this JSON. Records with
 * {@code enabled=false} are logical deletions and are published as any other record.</p>
 *
 * @param id          id assigned by the catedra; {@code null} when the payload omits it
 * @param name        display name
 * @param description optional description
 * @param enabled     logical state; {@code null} means the field was not sent
 * @param createdAt   instant of creation reported by the catedra
 * @param updatedAt   instant of the last change reported by the catedra
 */
public record RedisCategoryValue(Long id,
                                 String name,
                                 String description,
                                 Boolean enabled,
                                 Instant createdAt,
                                 Instant updatedAt) {
}
