package ar.edu.um.turnos.catalog.catalog.infrastructure.redis.dto;

import java.util.List;

/**
 * Content published in {@code catedra:sync:changes:{version}} (contract v1, section 14.5).
 *
 * <p>The three arrays hold the <strong>ids</strong> affected by the version, never the
 * records themselves; the current state of each id is read from the corresponding hash.
 * An absent array is read as an empty one: a version may legitimately touch nothing this
 * service stores.</p>
 *
 * @param version version these changes belong to; {@code null} when the payload omits it
 * @param changes ids grouped by collection
 */
public record RedisChangesPayload(Long version, Changes changes) {

    /**
     * @param professionalCategories affected category ids, may be null
     * @param professionals         affected professional ids, may be null
     * @param weeklySchedules       affected weekly schedule ids, may be null
     */
    public record Changes(List<Long> professionalCategories,
                          List<Long> professionals,
                          List<Long> weeklySchedules) {
    }
}
