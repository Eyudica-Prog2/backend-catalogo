package ar.edu.um.turnos.catalog.catalog.infrastructure.rest.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotCategoryResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotProfessionalResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotResponse;
import ar.edu.um.turnos.catalog.catalog.infrastructure.rest.dto.SnapshotWeeklyScheduleResponse;
import ar.edu.um.turnos.catalog.support.CatalogFixtures;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit test of the mapper between the payload of the catedra and the domain snapshot.
 *
 * <p>Structural omissions are left as they arrive for the domain rules to reject; the only
 * value this mapper refuses to invent is {@code enabled}, which the contract makes mandatory.</p>
 */
class SnapshotResponseMapperTest {

    private final SnapshotResponseMapper mapper = new SnapshotResponseMapper();

    @Test
    void mapsEveryCollectionIntoTheDomain() {
        SnapshotResponse response = new SnapshotResponse(
                7L,
                CatalogFixtures.INSTANT,
                List.of(new SnapshotCategoryResponse(10L, "Salud", "Profesionales de la salud",
                        true, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT)),
                List.of(new SnapshotProfessionalResponse(101L, 10L, "Ana", "Perez",
                        true, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT)),
                List.of(new SnapshotWeeklyScheduleResponse(1001L, 101L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(13, 0), 30,
                        true, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT)));

        CatalogSnapshot snapshot = mapper.toDomain(response);

        assertThat(snapshot.getSnapshotVersion()).isEqualTo(7L);
        assertThat(snapshot.getGeneratedAt()).isEqualTo(CatalogFixtures.INSTANT);
        assertThat(snapshot.getProfessionalCategories()).hasSize(1);
        assertThat(snapshot.getProfessionals()).hasSize(1);
        assertThat(snapshot.getWeeklySchedules()).hasSize(1);
        assertThat(snapshot.getWeeklySchedules().get(0).getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(snapshot.getWeeklySchedules().get(0).getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(snapshot.getWeeklySchedules().get(0).getSlotDurationMinutes()).isEqualTo(30);
        assertThat(snapshot.getWeeklySchedules().get(0).isEnabled()).isTrue();

        assertThatCode(snapshot::validate).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnEmptyPayload() {
        assertThatThrownBy(() -> mapper.toDomain(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void refusesToInventTheEnabledStateOfACategory() {
        SnapshotResponse response = responseWithCategory(new SnapshotCategoryResponse(
                10L, "Salud", null, null, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT));

        assertThatThrownBy(() -> mapper.toDomain(response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("professional category")
                .hasMessageContaining("enabled");
    }

    @Test
    void refusesToInventTheEnabledStateOfAProfessional() {
        SnapshotResponse response = responseWithProfessional(new SnapshotProfessionalResponse(
                101L, 10L, "Ana", "Perez", null, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT));

        assertThatThrownBy(() -> mapper.toDomain(response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A professional of the snapshot");
    }

    @Test
    void refusesToInventTheEnabledStateOfASchedule() {
        SnapshotResponse response = responseWithSchedule(new SnapshotWeeklyScheduleResponse(
                1001L, 101L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0), 30,
                null, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT));

        assertThatThrownBy(() -> mapper.toDomain(response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weekly schedule")
                .hasMessageContaining("enabled");
    }

    @Test
    void leavesMissingCollectionsAndValuesForTheDomainRulesToReject() {
        SnapshotResponse missingSchedules = new SnapshotResponse(
                7L, CatalogFixtures.INSTANT, List.of(), List.of(), null);
        assertThat(mapper.toDomain(missingSchedules).getWeeklySchedules()).isNull();

        SnapshotResponse missingVersion = new SnapshotResponse(
                null, null, List.of(), List.of(), List.of());
        assertThat(mapper.toDomain(missingVersion).getSnapshotVersion()).isZero();

        SnapshotResponse missingDuration = new SnapshotResponse(
                7L, null, List.of(), List.of(),
                List.of(new SnapshotWeeklyScheduleResponse(1001L, 101L, DayOfWeek.MONDAY,
                        LocalTime.of(9, 0), LocalTime.of(13, 0), null,
                        true, CatalogFixtures.INSTANT, CatalogFixtures.INSTANT)));
        assertThat(mapper.toDomain(missingDuration).getWeeklySchedules().get(0)
                .getSlotDurationMinutes()).isZero();
    }

    private static SnapshotResponse responseWithCategory(SnapshotCategoryResponse category) {
        return new SnapshotResponse(7L, null, List.of(category), List.of(), List.of());
    }

    private static SnapshotResponse responseWithProfessional(SnapshotProfessionalResponse professional) {
        return new SnapshotResponse(7L, null, List.of(), List.of(professional), List.of());
    }

    private static SnapshotResponse responseWithSchedule(SnapshotWeeklyScheduleResponse schedule) {
        return new SnapshotResponse(7L, null, List.of(), List.of(), List.of(schedule));
    }
}
