package ar.edu.unlp.turnos.catalog.support;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Publishes, in the Redis container of the tests, exactly what contract v1 section 14 says
 * the catedra publishes: the version window, the deltas and the current state of each record.
 *
 * <p>The payloads are written by hand instead of being produced by the mappers of the
 * service: this fixture plays the role of a third party, so it must be able to be right even
 * if the implementation changes.</p>
 *
 * <p>Every test is expected to start from {@link #clear()}, which removes both namespaces
 * (the published one and the private one of this project), because Redis is a real service
 * shared by every test of the run and it has no transaction to roll back.</p>
 */
public final class CatedraRedisFixture {

    /** Published namespace of this project, section 14 of the contract. */
    private static final String READ_PREFIX = "catedra:sync:";

    /** Private namespace of this project, section 14.6 of the contract. */
    private static final String WRITE_PREFIX = "alumnos:proyecto-test:";

    /** The records of the fixtures report this instant as creation and update time. */
    private static final String INSTANT = "2026-07-01T10:00:00Z";

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;

    private CatedraRedisFixture() {
    }

    /**
     * Publishes {@code current-version}, {@code oldest-available-version} and the metadata
     * hash of section 14.3 with the same values.
     *
     * @param currentVersion newest version whose changes are fully available
     * @param oldestVersion  oldest version whose changes are still available
     */
    public static void publishWindow(long currentVersion, long oldestVersion) {
        publishWindow(currentVersion, oldestVersion, INSTANT);
    }

    /**
     * Publishes the version window and the metadata hash.
     *
     * @param currentVersion newest version whose changes are fully available
     * @param oldestVersion  oldest version whose changes are still available
     * @param publishedAt    instant the window was published
     */
    public static void publishWindow(long currentVersion, long oldestVersion, String publishedAt) {
        RedisCommands<String, String> redis = commands();
        redis.set(READ_PREFIX + "current-version", Long.toString(currentVersion));
        redis.set(READ_PREFIX + "oldest-available-version", Long.toString(oldestVersion));
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("currentVersion", Long.toString(currentVersion));
        metadata.put("oldestAvailableVersion", Long.toString(oldestVersion));
        metadata.put("publishedAt", publishedAt);
        metadata.put("schemaVersion", "1");
        redis.hset(READ_PREFIX + "metadata", metadata);
    }

    /**
     * Publishes the delta of one version: the ids each collection touched, never the records
     * themselves (section 14.5).
     *
     * @param version       version of the delta
     * @param categories    ids of the categories touched
     * @param professionals ids of the professionals touched
     * @param schedules     ids of the schedules touched
     */
    public static void publishChanges(long version, List<Long> categories,
                                      List<Long> professionals, List<Long> schedules) {
        String body = "{\"version\":" + version
                + ",\"changes\":{\"professionalCategories\":" + ids(categories)
                + ",\"professionals\":" + ids(professionals)
                + ",\"weeklySchedules\":" + ids(schedules) + "}}";
        commands().set(READ_PREFIX + "changes:" + version, body);
    }

    /**
     * Publishes a delta that only touches the given professionals.
     *
     * @param version       version of the delta
     * @param professionalIds ids of the professionals touched
     */
    public static void publishChanges(long version, long... professionalIds) {
        List<Long> ids = new java.util.ArrayList<>();
        for (long id : professionalIds) {
            ids.add(id);
        }
        publishChanges(version, List.of(), ids, List.of());
    }

    /**
     * Publishes the current state of one professional in
     * {@code catedra:sync:professionals} (section 14.4).
     *
     * @param id          id of the professional
     * @param categoryId  category it belongs to
     * @param firstName   first name
     * @param lastName    last name
     * @param enabled     logical state
     */
    public static void publishProfessional(long id, long categoryId, String firstName,
                                           String lastName, boolean enabled) {
        publishRecord("professionals", id, professionalJson(id, categoryId, firstName, lastName, enabled));
    }

    /**
     * Publishes the current state of one professional category in
     * {@code catedra:sync:professional-categories}.
     *
     * @param id      id of the category
     * @param name    display name
     * @param enabled logical state
     */
    public static void publishCategory(long id, String name, boolean enabled) {
        publishRecord("professional-categories", id, categoryJson(id, name, enabled));
    }

    /**
     * Writes the current state of a record in the hash of its collection.
     *
     * @param hash name of the hash after the namespace, e.g. {@code professionals}
     * @param id   id of the record, used as field of the hash
     * @param json body published for that record
     */
    public static void publishRecord(String hash, long id, String json) {
        commands().hset(READ_PREFIX + hash, Long.toString(id), json);
    }

    /**
     * @param id          id of the professional
     * @param categoryId  category it belongs to
     * @param firstName   first name
     * @param lastName    last name
     * @param enabled     logical state
     * @return the body the catedra publishes for that professional
     */
    public static String professionalJson(long id, long categoryId, String firstName,
                                          String lastName, boolean enabled) {
        return "{\"id\":" + id
                + ",\"categoryId\":" + categoryId
                + ",\"firstName\":\"" + firstName + "\""
                + ",\"lastName\":\"" + lastName + "\""
                + ",\"enabled\":" + enabled
                + ",\"createdAt\":\"" + INSTANT + "\""
                + ",\"updatedAt\":\"" + INSTANT + "\"}";
    }

    /**
     * @param id      id of the category
     * @param name    display name
     * @param enabled logical state
     * @return the body the catedra publishes for that category
     */
    public static String categoryJson(long id, String name, boolean enabled) {
        return "{\"id\":" + id
                + ",\"name\":\"" + name + "\""
                + ",\"description\":null"
                + ",\"enabled\":" + enabled
                + ",\"createdAt\":\"" + INSTANT + "\""
                + ",\"updatedAt\":\"" + INSTANT + "\"}";
    }

    /**
     * Removes every key of both namespaces of this project: the published feed and the
     * private diagnostic. Redis has no rollback, so a test starts from here.
     */
    public static void clear() {
        RedisCommands<String, String> redis = commands();
        List<String> keys = redis.keys(READ_PREFIX + "*");
        keys.addAll(redis.keys(WRITE_PREFIX + "*"));
        if (!keys.isEmpty()) {
            redis.del(keys.toArray(String[]::new));
        }
    }

    /**
     * @return the body written in the private diagnostic of the last attempt, empty when no
     *         attempt has been recorded yet
     */
    public static Optional<String> lastSyncDiagnostic() {
        return Optional.ofNullable(commands().get(WRITE_PREFIX + "diagnostics:last-sync"));
    }

    /**
     * @param hash      name of the hash after the namespace
     * @param id        id of the record
     * @return the body published for that record, empty when it is not published
     */
    public static Optional<String> publishedRecord(String hash, long id) {
        return Optional.ofNullable(commands().hget(READ_PREFIX + hash, Long.toString(id)));
    }

    /**
     * @param version version whose delta must exist
     * @return the body published for that delta, empty when it is not published
     */
    public static Optional<String> publishedChanges(long version) {
        return Optional.ofNullable(commands().get(READ_PREFIX + "changes:" + version));
    }

    private static String ids(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        StringBuilder body = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                body.append(',');
            }
            body.append(values.get(i));
        }
        return body.append(']').toString();
    }

    private static RedisCommands<String, String> commands() {
        return connection().sync();
    }

    private static synchronized StatefulRedisConnection<String, String> connection() {
        if (connection != null && connection.isOpen()) {
            return connection;
        }
        if (client == null) {
            RedisURI uri = RedisURI.builder()
                    .withHost(TestEnvironment.redis().getHost())
                    .withPort(TestEnvironment.redis().getMappedPort(6379))
                    .build();
            client = RedisClient.create(uri);
        }
        connection = client.connect();
        return connection;
    }

}
