package ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto;

import java.time.Instant;

/**
 * Value written to the private namespace of this project,
 * {@code alumnos:{groupId}:diagnostics:last-sync} (contract v1, section 14.6).
 *
 * <p>Only the fields with a value are serialized (the service writes JSON with
 * {@code non_null}), so a successful attempt produces a short, readable breadcrumb and a
 * failed one carries the reason.</p>
 *
 * @param appliedVersion         version of the local copy when the attempt finished
 * @param currentVersion         window observed in Redis, null when it could not be read
 * @param oldestAvailableVersion window observed in Redis, null when it could not be read
 * @param result                 outcome of the attempt, null when it did not finish
 * @param detail                 self contained description of a failure, null on success
 * @param recordedAt             instant the diagnostic was written
 */
public record RedisDiagnosticValue(long appliedVersion,
                                   Long currentVersion,
                                   Long oldestAvailableVersion,
                                   String result,
                                   String detail,
                                   Instant recordedAt) {
}
