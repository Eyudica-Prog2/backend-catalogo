package ar.edu.unlp.turnos.catalog.catalog.infrastructure.redis.mapper;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogChanges;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.redis.dto.RedisCategoryValue;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.redis.dto.RedisChangesPayload;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.redis.dto.RedisProfessionalValue;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.redis.dto.RedisScheduleValue;
import org.springframework.stereotype.Component;

/**
 * Maps the JSON published by the catedra in Redis (contract v1, section 14) into the domain
 * model of the local copy.
 *
 * <p>Same rule as the snapshot mapper: structural omissions are mapped as they arrive and
 * the only thing this mapper refuses to do is invent a value the domain cannot represent.
 * {@code id} and {@code enabled} are mandatory, because without them the record cannot be
 * stored nor interpreted (an unknown {@code enabled} could silently resurrect or delete a
 * record of the local copy).</p>
 *
 * <p>{@code enabled=false} is mapped as a record like any other: it is a logical deletion
 * that must be applied, never dropped.</p>
 */
@Component
public class RedisPayloadMapper {

    /**
     * @param payload delta published for one version
     * @return the equivalent domain delta
     * @throws IllegalArgumentException when the payload does not declare its version
     */
    public CatalogChanges toDomain(RedisChangesPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("The changes published by Redis are empty.");
        }
        if (payload.version() == null) {
            throw new IllegalArgumentException("The changes published by Redis do not declare a version.");
        }
        RedisChangesPayload.Changes changes = payload.changes();
        if (changes == null) {
            // A version that does not declare any collection touched nothing this service
            // stores: it must still be applied so the local version can advance.
            return new CatalogChanges(payload.version(), null, null, null);
        }
        return new CatalogChanges(payload.version(), changes.professionalCategories(),
                changes.professionals(), changes.weeklySchedules());
    }

    /**
     * @param value current state published for one category
     * @return the equivalent domain record
     * @throws IllegalArgumentException when a mandatory field is missing
     */
    public ProfessionalCategory toDomain(RedisCategoryValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Redis published an empty professional category.");
        }
        requireEnabled(value.enabled(), "A professional category published by Redis");
        if (value.id() == null) {
            throw new IllegalArgumentException("A professional category published by Redis does not declare an id.");
        }
        return ProfessionalCategory.builder()
                .id(value.id())
                .name(value.name())
                .description(value.description())
                .enabled(value.enabled())
                .createdAt(value.createdAt())
                .updatedAt(value.updatedAt())
                .build();
    }

    /**
     * @param value current state published for one professional
     * @return the equivalent domain record
     * @throws IllegalArgumentException when a mandatory field is missing
     */
    public Professional toDomain(RedisProfessionalValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Redis published an empty professional.");
        }
        requireEnabled(value.enabled(), "A professional published by Redis");
        if (value.id() == null) {
            throw new IllegalArgumentException("A professional published by Redis does not declare an id.");
        }
        if (value.categoryId() == null) {
            throw new IllegalArgumentException(
                    "The professional " + value.id() + " published by Redis does not declare a categoryId.");
        }
        return Professional.builder()
                .id(value.id())
                .categoryId(value.categoryId())
                .firstName(value.firstName())
                .lastName(value.lastName())
                .enabled(value.enabled())
                .createdAt(value.createdAt())
                .updatedAt(value.updatedAt())
                .build();
    }

    /**
     * @param value current state published for one weekly schedule
     * @return the equivalent domain record
     * @throws IllegalArgumentException when a mandatory field is missing
     */
    public WeeklySchedule toDomain(RedisScheduleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Redis published an empty weekly schedule.");
        }
        requireEnabled(value.enabled(), "A weekly schedule published by Redis");
        if (value.id() == null) {
            throw new IllegalArgumentException("A weekly schedule published by Redis does not declare an id.");
        }
        if (value.professionalId() == null) {
            throw new IllegalArgumentException(
                    "The weekly schedule " + value.id() + " published by Redis does not declare a professionalId.");
        }
        if (value.slotDurationMinutes() == null) {
            throw new IllegalArgumentException(
                    "The weekly schedule " + value.id() + " published by Redis does not declare slotDurationMinutes.");
        }
        return WeeklySchedule.builder()
                .id(value.id())
                .professionalId(value.professionalId())
                .dayOfWeek(value.dayOfWeek())
                .startTime(value.startTime())
                .endTime(value.endTime())
                .slotDurationMinutes(value.slotDurationMinutes())
                .enabled(value.enabled())
                .createdAt(value.createdAt())
                .updatedAt(value.updatedAt())
                .build();
    }

    private static void requireEnabled(Boolean enabled, String subject) {
        if (enabled == null) {
            throw new IllegalArgumentException(subject + " does not declare the required field enabled.");
        }
    }
}
