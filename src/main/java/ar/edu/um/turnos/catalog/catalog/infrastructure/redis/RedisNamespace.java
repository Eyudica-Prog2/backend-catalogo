package ar.edu.um.turnos.catalog.catalog.infrastructure.redis;

/**
 * Turns a namespace received from the catedra into the keys this service reads and writes.
 *
 * <p>The contract delivers namespaces as patterns ({@code catedra:sync:*} for reading and
 * {@code alumnos:{groupId}:*} for writing, section 14.1). The pattern is stripped of its
 * trailing wildcard so both ends of every key are built from one single configured value:
 * nothing is hardcoded and the private namespace of this project cannot leak into the
 * published one.</p>
 *
 * <p>Immutable and thread safe.</p>
 */
final class RedisNamespace {

    private final String prefix;

    /**
     * @param namespace namespace as delivered by the catedra, e.g. {@code catedra:sync:*}
     * @throws IllegalArgumentException when the namespace is blank and cannot produce keys
     */
    RedisNamespace(String namespace) {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("A Redis namespace is required to build keys.");
        }
        String trimmed = namespace.trim();
        while (trimmed.endsWith("*") || trimmed.endsWith(":")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(
                    "The Redis namespace '" + namespace + "' does not identify any key.");
        }
        this.prefix = trimmed;
    }

    /**
     * @param suffix part of the key after the namespace, without the leading colon
     * @return the full key, e.g. {@code catedra:sync:current-version}
     */
    String key(String suffix) {
        return prefix + ":" + suffix;
    }

    /**
     * @return the namespace without its wildcard, e.g. {@code catedra:sync}
     */
    String prefix() {
        return prefix;
    }
}
