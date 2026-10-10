package ar.edu.um.turnos.catalog.catalog.domain.model;

import ar.edu.um.turnos.catalog.shared.util.Strings;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Complete catalog snapshot as published by the catedra (contract v1, section 7).
 *
 * <p>It is the unit of work of the full synchronization: the three collections and the
 * version they belong to. The local copy only advances its applied version when the whole
 * snapshot was applied, which is why the snapshot is validated as a whole.</p>
 *
 * <p>Pure domain model: no Jackson, no JPA, no Spring. Its invariants are checked by
 * {@link #validate()}, which throws {@link IllegalArgumentException} with a self contained
 * message; the application layer turns that message into {@code 503 SNAPSHOT_INVALID}.</p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class CatalogSnapshot {

    private long snapshotVersion;
    private Instant generatedAt;
    private List<ProfessionalCategory> professionalCategories;
    private List<Professional> professionals;
    private List<WeeklySchedule> weeklySchedules;

    /**
     * Checks that the snapshot can be stored consistently: complete collections, unique and
     * positive remote ids, referential integrity between the three collections and usable
     * time ranges.
     *
     * @throws IllegalArgumentException with a self contained message when it cannot be applied
     */
    public void validate() {
        if (snapshotVersion <= 0) {
            throw new IllegalArgumentException(
                    "The snapshot does not declare a positive snapshotVersion.");
        }
        requireCollections();

        Set<Long> categoryIds = validateCategories();
        Set<Long> professionalIds = validateProfessionals(categoryIds);
        validateWeeklySchedules(professionalIds);
    }

    private void requireCollections() {
        if (professionalCategories == null) {
            throw new IllegalArgumentException(
                    "The snapshot does not contain the professionalCategories collection.");
        }
        if (professionals == null) {
            throw new IllegalArgumentException(
                    "The snapshot does not contain the professionals collection.");
        }
        if (weeklySchedules == null) {
            throw new IllegalArgumentException(
                    "The snapshot does not contain the weeklySchedules collection.");
        }
    }

    private Set<Long> validateCategories() {
        Set<Long> ids = new HashSet<>();
        for (ProfessionalCategory category : professionalCategories) {
            if (category == null) {
                throw new IllegalArgumentException(
                        "The professionalCategories collection contains an empty entry.");
            }
            Long id = requireRemoteId(category.getId(), "professional category");
            if (Strings.isBlank(category.getName())) {
                throw new IllegalArgumentException(
                        "The professional category " + id + " does not declare a name.");
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException(
                        "The professional category id " + id + " appears more than once in the snapshot.");
            }
        }
        return ids;
    }

    private Set<Long> validateProfessionals(Set<Long> categoryIds) {
        Set<Long> ids = new HashSet<>();
        for (Professional professional : professionals) {
            if (professional == null) {
                throw new IllegalArgumentException(
                        "The professionals collection contains an empty entry.");
            }
            Long id = requireRemoteId(professional.getId(), "professional");
            if (professional.getCategoryId() == null) {
                throw new IllegalArgumentException(
                        "The professional " + id + " does not declare a categoryId.");
            }
            if (!categoryIds.contains(professional.getCategoryId())) {
                throw new IllegalArgumentException(
                        "The professional " + id + " refers to the category "
                                + professional.getCategoryId()
                                + ", which is not part of the snapshot.");
            }
            if (Strings.isBlank(professional.getFirstName())
                    || Strings.isBlank(professional.getLastName())) {
                throw new IllegalArgumentException(
                        "The professional " + id + " does not declare a first and last name.");
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException(
                        "The professional id " + id + " appears more than once in the snapshot.");
            }
        }
        return ids;
    }

    private void validateWeeklySchedules(Set<Long> professionalIds) {
        Set<Long> ids = new HashSet<>();
        for (WeeklySchedule schedule : weeklySchedules) {
            if (schedule == null) {
                throw new IllegalArgumentException(
                        "The weeklySchedules collection contains an empty entry.");
            }
            Long id = requireRemoteId(schedule.getId(), "weekly schedule");
            if (schedule.getProfessionalId() == null || !professionalIds.contains(schedule.getProfessionalId())) {
                throw new IllegalArgumentException(
                        "The weekly schedule " + id + " refers to the professional "
                                + schedule.getProfessionalId()
                                + ", which is not part of the snapshot.");
            }
            if (schedule.getDayOfWeek() == null) {
                throw new IllegalArgumentException(
                        "The weekly schedule " + id + " does not declare a dayOfWeek.");
            }
            if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
                throw new IllegalArgumentException(
                        "The weekly schedule " + id + " must declare both startTime and endTime.");
            }
            if (!schedule.getStartTime().isBefore(schedule.getEndTime())) {
                throw new IllegalArgumentException(
                        "The weekly schedule " + id + " must start before it ends.");
            }
            if (schedule.getSlotDurationMinutes() <= 0) {
                throw new IllegalArgumentException(
                        "The weekly schedule " + id + " must declare a positive slotDurationMinutes.");
            }
            if (!ids.add(id)) {
                throw new IllegalArgumentException(
                        "The weekly schedule id " + id + " appears more than once in the snapshot.");
            }
        }
    }

    private static Long requireRemoteId(Long id, String entity) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Every " + entity + " of the snapshot must declare a positive id.");
        }
        return id;
    }
}
