package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.adapter;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSyncState;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogVersionWindow;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.SyncResult;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import ar.edu.unlp.turnos.catalog.catalog.domain.ports.out.CatalogSyncRepository;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.CatalogSyncStateEntity;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalCategoryEntity;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.ProfessionalEntity;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity.WeeklyScheduleEntity;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.mapper.CatalogMapper;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository.JpaCatalogSyncStateRepository;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository.JpaProfessionalCategoryRepository;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository.JpaProfessionalRepository;
import ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.repository.JpaWeeklyScheduleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for the {@link CatalogSyncRepository} port: the writes that must be atomic.
 *
 * <p>{@link #applySnapshot} replaces the three collections and advances the applied version
 * inside one transaction, so a failure in any of them leaves both the data and the version
 * exactly as they were (contract v1, section 7). {@link #applyIncremental} does the same for
 * one incremental version: its changes and the applied version move together, which is what
 * makes a reprocessed version harmless (contract v1, sections 14.5 and 18.1).</p>
 *
 * <p>The order of the statements is dictated by the foreign keys: delete children before
 * parents, insert parents before children.</p>
 */
@Component
@RequiredArgsConstructor
public class JpaCatalogSyncRepositoryAdapter implements CatalogSyncRepository {

    /** Column {@code catalog_sync_state.last_error} is {@code VARCHAR(1000)}. */
    private static final int MAX_ERROR_LENGTH = 1000;

    private final JpaProfessionalCategoryRepository categoryRepository;
    private final JpaProfessionalRepository professionalRepository;
    private final JpaWeeklyScheduleRepository weeklyScheduleRepository;
    private final JpaCatalogSyncStateRepository stateRepository;
    private final CatalogMapper mapper;

    /**
     * Explicit because this adapter needs to control when the persistence context is
     * flushed and cleared; see {@link #applySnapshot}.
     */
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogSyncState> findState() {
        return stateRepository.findFirstByOrderByIdAsc().map(mapper::toDomain);
    }

    @Override
    @Transactional
    public CatalogSyncState applySnapshot(CatalogSnapshot snapshot) {
        // Write whatever the caller had pending before deleting anything: a bulk delete does
        // not flush on its own and those rows would survive this replacement.
        entityManager.flush();

        weeklyScheduleRepository.deleteAllInBatch();
        professionalRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();

        // Bulk deletes bypass the persistence context: without this clear, the ids that were
        // just deleted would still be considered "present" and the inserts below would be
        // turned into updates of rows that no longer exist (silent data loss).
        entityManager.clear();

        snapshot.getProfessionalCategories()
                .forEach(category -> entityManager.persist(mapper.toEntity(category)));
        snapshot.getProfessionals()
                .forEach(professional -> entityManager.persist(mapper.toEntity(professional)));
        snapshot.getWeeklySchedules()
                .forEach(schedule -> entityManager.persist(mapper.toEntity(schedule)));

        Instant appliedAt = Instant.now();
        CatalogSyncStateEntity state = currentState();
        state.setSnapshotVersion(snapshot.getSnapshotVersion());
        state.setAppliedVersion(snapshot.getSnapshotVersion());
        state.setLastSyncAt(appliedAt);
        state.setLastError(null);
        state.setLastErrorAt(null);
        state.setLastSyncResult(SyncResult.SNAPSHOT_APPLIED);
        CatalogSyncStateEntity saved = stateRepository.save(state);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional
    public CatalogSyncState applyIncremental(long version, List<ProfessionalCategory> categories,
                                             List<Professional> professionals,
                                             List<WeeklySchedule> schedules) {
        // Upserts are written parents first (foreign keys) and children after. The remote ids
        // are the primary keys, so applying the same version twice rewrites the same rows and
        // produces exactly the same state (idempotency of the incremental application).
        categories.forEach(category -> upsertCategory(category));
        professionals.forEach(professional -> upsertProfessional(professional));
        schedules.forEach(schedule -> upsertWeeklySchedule(schedule));

        CatalogSyncStateEntity state = currentState();
        state.setSnapshotVersion(version);
        state.setAppliedVersion(version);
        state.setLastSyncAt(Instant.now());
        state.setLastError(null);
        state.setLastErrorAt(null);
        state.setLastSyncResult(SyncResult.INCREMENTAL_APPLIED);
        return mapper.toDomain(stateRepository.save(state));
    }

    @Override
    @Transactional
    public void recordVersionWindow(CatalogVersionWindow window) {
        CatalogSyncStateEntity state = currentState();
        state.setCurrentVersion(window.getCurrentVersion());
        state.setOldestAvailableVersion(window.getOldestAvailableVersion());
        stateRepository.save(state);
    }

    @Override
    @Transactional
    public void recordSuccess(SyncResult result, Instant succeededAt) {
        CatalogSyncStateEntity state = currentState();
        state.setLastSyncResult(result);
        state.setLastSyncAt(succeededAt);
        state.setLastError(null);
        state.setLastErrorAt(null);
        stateRepository.save(state);
    }

    @Override
    @Transactional
    public void recordFailure(Long attemptedSnapshotVersion, String error, Instant failedAt) {
        CatalogSyncStateEntity state = currentState();
        if (attemptedSnapshotVersion != null) {
            // The version was received but could not be applied: the applied version is
            // deliberately left untouched, this is what makes the gap visible in the status.
            state.setSnapshotVersion(attemptedSnapshotVersion);
        }
        state.setLastSyncResult(SyncResult.FAILED);
        state.setLastError(truncate(error));
        state.setLastErrorAt(failedAt);
        stateRepository.save(state);
    }

    /**
     * Writes the current state of one published category over the row with the same remote
     * id (or inserts it when this version publishes it for the first time).
     */
    private void upsertCategory(ProfessionalCategory category) {
        ProfessionalCategoryEntity entity = categoryRepository.findById(category.getId())
                .orElseGet(ProfessionalCategoryEntity::new);
        entity.setId(category.getId());
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        entity.setEnabled(category.isEnabled());
        entity.setCreatedAt(category.getCreatedAt());
        entity.setUpdatedAt(category.getUpdatedAt());
        categoryRepository.save(entity);
    }

    private void upsertProfessional(Professional professional) {
        ProfessionalEntity entity = professionalRepository.findById(professional.getId())
                .orElseGet(ProfessionalEntity::new);
        entity.setId(professional.getId());
        entity.setCategoryId(professional.getCategoryId());
        entity.setFirstName(professional.getFirstName());
        entity.setLastName(professional.getLastName());
        entity.setEnabled(professional.isEnabled());
        entity.setCreatedAt(professional.getCreatedAt());
        entity.setUpdatedAt(professional.getUpdatedAt());
        professionalRepository.save(entity);
    }

    private void upsertWeeklySchedule(WeeklySchedule schedule) {
        WeeklyScheduleEntity entity = weeklyScheduleRepository.findById(schedule.getId())
                .orElseGet(WeeklyScheduleEntity::new);
        entity.setId(schedule.getId());
        entity.setProfessionalId(schedule.getProfessionalId());
        entity.setDayOfWeek(schedule.getDayOfWeek());
        entity.setStartTime(schedule.getStartTime());
        entity.setEndTime(schedule.getEndTime());
        entity.setSlotDurationMinutes(schedule.getSlotDurationMinutes());
        entity.setEnabled(schedule.isEnabled());
        entity.setCreatedAt(schedule.getCreatedAt());
        entity.setUpdatedAt(schedule.getUpdatedAt());
        weeklyScheduleRepository.save(entity);
    }

    private CatalogSyncStateEntity currentState() {
        return stateRepository.findFirstByOrderByIdAsc().orElseGet(CatalogSyncStateEntity::new);
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
