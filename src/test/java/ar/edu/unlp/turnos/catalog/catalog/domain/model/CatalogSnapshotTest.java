package ar.edu.unlp.turnos.catalog.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.unlp.turnos.catalog.support.CatalogFixtures;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit test of the rules that decide whether a snapshot can replace the local copy.
 *
 * <p>Every message asserted here is self contained: it is what the client of
 * {@code POST /api/internal/sync/force} reads, so it must explain the problem on its own.</p>
 */
class CatalogSnapshotTest {

    @Test
    void acceptsASnapshotThatMatchesTheContract() {
        assertThatCode(() -> CatalogFixtures.snapshot(7).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsASnapshotWithoutVersion() {
        assertRejected(CatalogFixtures.snapshot(7).toBuilder().snapshotVersion(0).build(),
                "positive snapshotVersion");
    }

    @Test
    void rejectsASnapshotWithAMissingCollection() {
        assertRejected(CatalogFixtures.snapshot(7).toBuilder().weeklySchedules(null).build(),
                "weeklySchedules collection");
        assertRejected(CatalogFixtures.snapshot(7).toBuilder().professionals(null).build(),
                "professionals collection");
        assertRejected(CatalogFixtures.snapshot(7).toBuilder().professionalCategories(null).build(),
                "professionalCategories collection");
    }

    @Test
    void rejectsAnEmptyEntryInACollection() {
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .professionalCategories(Collections.singletonList(null)).build(),
                "empty entry");
    }

    @Test
    void rejectsACategoryWithoutUsableData() {
        ProfessionalCategory withoutId = CatalogFixtures.snapshot(7).getProfessionalCategories().get(0)
                .toBuilder().id(null).build();
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .professionalCategories(List.of(withoutId)).build(),
                "positive id");

        ProfessionalCategory withoutName = CatalogFixtures.snapshot(7).getProfessionalCategories().get(0)
                .toBuilder().name("  ").build();
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .professionalCategories(List.of(withoutName)).build(),
                "does not declare a name");
    }

    @Test
    void rejectsDuplicatedRemoteIds() {
        ProfessionalCategory category = CatalogFixtures.snapshot(7).getProfessionalCategories().get(0);
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .professionalCategories(List.of(category, category)).build(),
                "appears more than once");
    }

    @Test
    void rejectsAProfessionalThatDoesNotBelongToAnyCategoryOfTheSnapshot() {
        assertRejected(CatalogFixtures.snapshotWithUnknownCategory(7),
                "not part of the snapshot");
    }

    @Test
    void rejectsAProfessionalWithoutNames() {
        Professional professional = CatalogFixtures.snapshot(7).getProfessionals().get(0)
                .toBuilder().firstName(null).build();
        assertRejected(CatalogFixtures.snapshot(7).toBuilder().professionals(List.of(professional)).build(),
                "first and last name");
    }

    @Test
    void rejectsAScheduleOfAProfessionalThatIsNotInTheSnapshot() {
        WeeklySchedule schedule = CatalogFixtures.snapshot(7).getWeeklySchedules().get(0)
                .toBuilder().professionalId(999L).build();
        assertRejected(CatalogFixtures.snapshot(7).toBuilder().weeklySchedules(List.of(schedule)).build(),
                "which is not part of the snapshot");
    }

    @Test
    void rejectsAScheduleThatDoesNotEndAfterItStarts() {
        assertRejected(CatalogFixtures.snapshotWithBrokenTimeRange(7), "start before it ends");
    }

    @Test
    void rejectsAScheduleWithoutUsableTimesOrDuration() {
        WeeklySchedule base = CatalogFixtures.snapshot(7).getWeeklySchedules().get(0);

        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .weeklySchedules(List.of(base.toBuilder().startTime(null).build())).build(),
                "both startTime and endTime");
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .weeklySchedules(List.of(base.toBuilder().dayOfWeek(null).build())).build(),
                "dayOfWeek");
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .weeklySchedules(List.of(base.toBuilder().slotDurationMinutes(0).build())).build(),
                "positive slotDurationMinutes");
        assertRejected(CatalogFixtures.snapshot(7).toBuilder()
                        .weeklySchedules(List.of(base.toBuilder()
                                .startTime(LocalTime.of(13, 0)).build())).build(),
                "start before it ends");
    }

    private static void assertRejected(CatalogSnapshot snapshot, String expectedFragment) {
        assertThatThrownBy(snapshot::validate)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(expectedFragment);
    }
}
