package ar.edu.unlp.turnos.catalog.catalog.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

/**
 * Body of {@code GET /api/synchronization/snapshot} (contract v1, section 7).
 *
 * <p>Transport representation only: it is mapped into the domain model
 * {@code CatalogSnapshot} and never leaves this package. Fields are nullable on purpose, so
 * a payload that is missing something can be rejected by the domain rules instead of being
 * silently defaulted.</p>
 *
 * @param snapshotVersion       version published by the catedra
 * @param generatedAt           instant the snapshot was generated
 * @param professionalCategories categories, enabled and disabled
 * @param professionals         professionals, enabled and disabled
 * @param weeklySchedules       weekly schedules, enabled and disabled
 */
public record SnapshotResponse(Long snapshotVersion,
                               Instant generatedAt,
                               List<SnapshotCategoryResponse> professionalCategories,
                               List<SnapshotProfessionalResponse> professionals,
                               List<SnapshotWeeklyScheduleResponse> weeklySchedules) {
}
