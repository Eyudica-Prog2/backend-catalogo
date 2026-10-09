package ar.edu.unlp.turnos.catalog.support;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.CatalogSnapshot;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.Professional;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalCategory;
import ar.edu.unlp.turnos.catalog.catalog.domain.model.WeeklySchedule;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Catalog snapshots used by the tests.
 *
 * <p>The default snapshot is deliberately richer than the minimum: five professionals of two
 * categories with a mix of enabled, disabled and missing schedules, so every filter of the
 * statement (category, name, enabled state and availability) has a distinct answer:</p>
 *
 * <pre>
 * id    category  name              enabled  schedules                      available
 * 101   10        Ana Perez         yes      MON enabled, SAT disabled      yes
 * 102   10        Bruno Gomez       yes      TUE disabled                   no
 * 103   20        Carla Diaz        no       none                           no
 * 104   10        Ana Ruiz          yes      WED enabled                    yes
 * 105   20        Diego Sosa        yes      TUE and THU enabled            yes
 * </pre>
 *
 * <p>Sorted by last name, the order of the default page is Diaz, Gomez, Perez, Ruiz, Sosa.</p>
 */
public final class CatalogFixtures {

    public static final long HEALTH_CATEGORY = 10L;
    public static final long SPORT_CATEGORY = 20L;

    public static final long ANA_PEREZ = 101L;
    public static final long BRUNO_GOMEZ = 102L;
    public static final long CARLA_DIAZ = 103L;
    public static final long ANA_RUIZ = 104L;
    public static final long DIEGO_SOSA = 105L;

    public static final long MONDAY_SCHEDULE = 1001L;
    public static final long SATURDAY_SCHEDULE = 1002L;
    public static final long TUESDAY_SCHEDULE = 1003L;
    public static final long WEDNESDAY_SCHEDULE = 1004L;
    public static final long TUESDAY_SOSA_SCHEDULE = 1005L;
    public static final long THURSDAY_SOSA_SCHEDULE = 1006L;

    /** Instant every record of the fixtures reports as creation and update time. */
    public static final Instant INSTANT = Instant.parse("2026-07-01T10:00:00Z");

    private CatalogFixtures() {
    }

    /**
     * @param version version of the snapshot
     * @return a snapshot that passes every domain rule
     */
    public static CatalogSnapshot snapshot(long version) {
        return CatalogSnapshot.builder()
                .snapshotVersion(version)
                .generatedAt(INSTANT)
                .professionalCategories(categories())
                .professionals(professionals())
                .weeklySchedules(schedules())
                .build();
    }

    /**
     * @param version version of the snapshot
     * @return a snapshot whose first professional points to a category that is not part of it
     */
    public static CatalogSnapshot snapshotWithUnknownCategory(long version) {
        List<Professional> professionals = new ArrayList<>(snapshot(version).getProfessionals());
        professionals.set(0, professionals.get(0).toBuilder().categoryId(999L).build());
        return snapshot(version).toBuilder().professionals(professionals).build();
    }

    /**
     * @param version version of the snapshot
     * @return a snapshot whose Monday schedule starts and ends at the same time
     */
    public static CatalogSnapshot snapshotWithBrokenTimeRange(long version) {
        List<WeeklySchedule> schedules = new ArrayList<>(snapshot(version).getWeeklySchedules());
        schedules.set(0, schedules.get(0).toBuilder().endTime(LocalTime.of(9, 0)).build());
        return snapshot(version).toBuilder().weeklySchedules(schedules).build();
    }

    private static List<ProfessionalCategory> categories() {
        return List.of(
                category(HEALTH_CATEGORY, "Salud", "Profesionales de la salud", true),
                category(SPORT_CATEGORY, "Deporte", null, false));
    }

    private static List<Professional> professionals() {
        return List.of(
                professional(ANA_PEREZ, HEALTH_CATEGORY, "Ana", "Perez", true),
                professional(BRUNO_GOMEZ, HEALTH_CATEGORY, "Bruno", "Gomez", true),
                professional(CARLA_DIAZ, SPORT_CATEGORY, "Carla", "Diaz", false),
                professional(ANA_RUIZ, HEALTH_CATEGORY, "Ana", "Ruiz", true),
                professional(DIEGO_SOSA, SPORT_CATEGORY, "Diego", "Sosa", true));
    }

    private static List<WeeklySchedule> schedules() {
        return List.of(
                schedule(MONDAY_SCHEDULE, ANA_PEREZ, DayOfWeek.MONDAY, "09:00", "13:00", 30, true),
                schedule(SATURDAY_SCHEDULE, ANA_PEREZ, DayOfWeek.SATURDAY, "10:00", "12:00", 60, false),
                schedule(TUESDAY_SCHEDULE, BRUNO_GOMEZ, DayOfWeek.TUESDAY, "08:00", "12:00", 30, false),
                schedule(WEDNESDAY_SCHEDULE, ANA_RUIZ, DayOfWeek.WEDNESDAY, "14:00", "18:00", 60, true),
                schedule(TUESDAY_SOSA_SCHEDULE, DIEGO_SOSA, DayOfWeek.TUESDAY, "09:00", "11:00", 60, true),
                schedule(THURSDAY_SOSA_SCHEDULE, DIEGO_SOSA, DayOfWeek.THURSDAY, "09:00", "11:00", 60, true));
    }

    private static ProfessionalCategory category(long id, String name, String description, boolean enabled) {
        return ProfessionalCategory.builder()
                .id(id)
                .name(name)
                .description(description)
                .enabled(enabled)
                .createdAt(INSTANT)
                .updatedAt(INSTANT)
                .build();
    }

    private static Professional professional(long id, long categoryId, String firstName,
                                             String lastName, boolean enabled) {
        return Professional.builder()
                .id(id)
                .categoryId(categoryId)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(enabled)
                .createdAt(INSTANT)
                .updatedAt(INSTANT)
                .build();
    }

    private static WeeklySchedule schedule(long id, long professionalId, DayOfWeek dayOfWeek,
                                           String start, String end, int slotMinutes, boolean enabled) {
        return WeeklySchedule.builder()
                .id(id)
                .professionalId(professionalId)
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .slotDurationMinutes(slotMinutes)
                .enabled(enabled)
                .createdAt(INSTANT)
                .updatedAt(INSTANT)
                .build();
    }
}
