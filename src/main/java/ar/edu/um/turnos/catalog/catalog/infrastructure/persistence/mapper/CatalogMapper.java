package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.mapper;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import org.springframework.stereotype.Component;

/**
 * Maps the domain of the local copy to the JPA entities and back.
 *
 * <p>One single persistence mapper on purpose: the three collections are always written
 * together (they are replaced as a whole inside one transaction) and splitting them would
 * only spread the same mapping over several files. It never crosses layers: it only knows
 * domain models and entities of this slice.</p>
 */
@Component
public class CatalogMapper {

    /**
     * @param entity persisted row
     * @return domain representation
     */
    public ProfessionalCategory toDomain(ProfessionalCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return ProfessionalCategory.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * @param domain domain model
     * @return a brand new entity, ready to be persisted with its remote id
     */
    public ProfessionalCategoryEntity toEntity(ProfessionalCategory domain) {
        ProfessionalCategoryEntity entity = new ProfessionalCategoryEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setEnabled(domain.isEnabled());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    /**
     * @param entity persisted row
     * @return domain representation
     */
    public Professional toDomain(ProfessionalEntity entity) {
        if (entity == null) {
            return null;
        }
        return Professional.builder()
                .id(entity.getId())
                .categoryId(entity.getCategoryId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * @param domain domain model
     * @return a brand new entity, ready to be persisted with its remote id
     */
    public ProfessionalEntity toEntity(Professional domain) {
        ProfessionalEntity entity = new ProfessionalEntity();
        entity.setId(domain.getId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setEnabled(domain.isEnabled());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    /**
     * @param entity persisted row
     * @return domain representation
     */
    public WeeklySchedule toDomain(WeeklyScheduleEntity entity) {
        if (entity == null) {
            return null;
        }
        return WeeklySchedule.builder()
                .id(entity.getId())
                .professionalId(entity.getProfessionalId())
                .dayOfWeek(entity.getDayOfWeek())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .slotDurationMinutes(entity.getSlotDurationMinutes())
                .enabled(entity.isEnabled())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * @param domain domain model
     * @return a brand new entity, ready to be persisted with its remote id
     */
    public WeeklyScheduleEntity toEntity(WeeklySchedule domain) {
        WeeklyScheduleEntity entity = new WeeklyScheduleEntity();
        entity.setId(domain.getId());
        entity.setProfessionalId(domain.getProfessionalId());
        entity.setDayOfWeek(domain.getDayOfWeek());
        entity.setStartTime(domain.getStartTime());
        entity.setEndTime(domain.getEndTime());
        entity.setSlotDurationMinutes(domain.getSlotDurationMinutes());
        entity.setEnabled(domain.isEnabled());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    /**
     * @param entity persisted row
     * @return domain representation
     */
    public CatalogSyncState toDomain(CatalogSyncStateEntity entity) {
        if (entity == null) {
            return null;
        }
        return CatalogSyncState.builder()
                .snapshotVersion(entity.getSnapshotVersion())
                .appliedVersion(entity.getAppliedVersion())
                .lastSyncAt(entity.getLastSyncAt())
                .lastError(entity.getLastError())
                .lastErrorAt(entity.getLastErrorAt())
                .lastSyncResult(entity.getLastSyncResult())
                .currentVersion(entity.getCurrentVersion())
                .oldestAvailableVersion(entity.getOldestAvailableVersion())
                .build();
    }
}
