package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.um.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.um.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.um.turnos.catalog.support.CatalogFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Unit test of the persistence mapper: a domain value that is written and read back must come
 * back unchanged, and a missing row must be represented as {@code null} instead of failing.
 */
class CatalogMapperTest {

    private final CatalogMapper mapper = new CatalogMapper();

    @Test
    void roundTripsACategory() {
        ProfessionalCategory category =
                CatalogFixtures.snapshot(1).getProfessionalCategories().get(0);

        ProfessionalCategoryEntity entity = mapper.toEntity(category);

        assertThat(entity.getId()).isEqualTo(category.getId());
        assertThat(entity.getName()).isEqualTo(category.getName());
        assertThat(entity.getDescription()).isEqualTo(category.getDescription());
        assertThat(entity.isEnabled()).isEqualTo(category.isEnabled());
        assertThat(entity.getCreatedAt()).isEqualTo(category.getCreatedAt());

        assertThat(mapper.toDomain(entity)).usingRecursiveComparison().isEqualTo(category);
    }

    @Test
    void roundTripsAProfessional() {
        Professional professional = CatalogFixtures.snapshot(1).getProfessionals().get(0);

        ProfessionalEntity entity = mapper.toEntity(professional);

        assertThat(entity.getCategoryId()).isEqualTo(professional.getCategoryId());
        assertThat(entity.getFirstName()).isEqualTo(professional.getFirstName());
        assertThat(entity.getLastName()).isEqualTo(professional.getLastName());
        assertThat(entity.isEnabled()).isEqualTo(professional.isEnabled());

        assertThat(mapper.toDomain(entity)).usingRecursiveComparison().isEqualTo(professional);
    }

    @Test
    void roundTripsAWeeklySchedule() {
        WeeklySchedule schedule = CatalogFixtures.snapshot(1).getWeeklySchedules().get(0);

        WeeklyScheduleEntity entity = mapper.toEntity(schedule);

        assertThat(entity.getDayOfWeek()).isEqualTo(schedule.getDayOfWeek());
        assertThat(entity.getStartTime()).isEqualTo(schedule.getStartTime());
        assertThat(entity.getEndTime()).isEqualTo(schedule.getEndTime());
        assertThat(entity.getSlotDurationMinutes()).isEqualTo(schedule.getSlotDurationMinutes());
        assertThat(entity.getProfessionalId()).isEqualTo(schedule.getProfessionalId());

        assertThat(mapper.toDomain(entity)).usingRecursiveComparison().isEqualTo(schedule);
    }

    @Test
    void mapsTheSynchronizationState() {
        CatalogSyncStateEntity entity = new CatalogSyncStateEntity();
        entity.setSnapshotVersion(9L);
        entity.setAppliedVersion(8L);
        entity.setLastSyncAt(Instant.parse("2026-07-01T10:00:00Z"));
        entity.setLastError("The snapshot published by the catedra could not be parsed.");
        entity.setLastErrorAt(Instant.parse("2026-07-01T10:05:00Z"));

        CatalogSyncState state = mapper.toDomain(entity);

        assertThat(state.getSnapshotVersion()).isEqualTo(9L);
        assertThat(state.getAppliedVersion()).isEqualTo(8L);
        assertThat(state.getLastSyncAt()).isEqualTo(entity.getLastSyncAt());
        assertThat(state.getLastError()).isEqualTo(entity.getLastError());
        assertThat(state.getLastErrorAt()).isEqualTo(entity.getLastErrorAt());
        assertThat(state.isSynced()).isTrue();
        assertThat(state.hasError()).isTrue();
    }

    @Test
    void representsAMissingRowAsNull() {
        assertThat(mapper.toDomain((ProfessionalCategoryEntity) null)).isNull();
        assertThat(mapper.toDomain((ProfessionalEntity) null)).isNull();
        assertThat(mapper.toDomain((WeeklyScheduleEntity) null)).isNull();
        assertThat(mapper.toDomain((CatalogSyncStateEntity) null)).isNull();
    }
}
