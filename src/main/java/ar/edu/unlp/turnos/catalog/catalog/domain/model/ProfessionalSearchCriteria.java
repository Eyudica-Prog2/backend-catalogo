package ar.edu.unlp.turnos.catalog.catalog.domain.model;

import ar.edu.unlp.turnos.catalog.shared.util.Strings;
import java.util.Locale;
import java.util.Set;

/**
 * Criteria of {@code GET /api/professionals}.
 *
 * <p>All the mandatory filters of the statement are represented here and are resolved over
 * the local copy of the catalog: category, name (partial match on first and last name),
 * enabled state and availability.</p>
 *
 * <p>The canonical constructor validates and normalizes the values, so the rule lives in the
 * domain: the HTTP layer only translates the failure into {@code 400 VALIDATION_ERROR}.</p>
 *
 * @param categoryId    optional category filter
 * @param name          optional partial name, trimmed; {@code null} when blank
 * @param enabled       optional enabled state filter
 * @param available     optional availability filter: {@code true} means "professional with at
 *                      least one enabled weekly schedule"
 * @param page          zero based page index, never negative
 * @param size          page size, between 1 and {@value #MAX_SIZE}
 * @param sortProperty  property to order by, one of the values of {@link #SORTABLE_PROPERTIES}
 * @param sortDirection direction of the order
 */
public record ProfessionalSearchCriteria(Long categoryId,
                                         String name,
                                         Boolean enabled,
                                         Boolean available,
                                         int page,
                                         int size,
                                         String sortProperty,
                                         SortDirection sortDirection) {

    /** Upper bound of {@code size}: a page bigger than this is always a client mistake. */
    public static final int MAX_SIZE = 100;

    /** Default page size when the client does not send one. */
    public static final int DEFAULT_SIZE = 20;

    /** Properties a client is allowed to sort by. */
    public static final Set<String> SORTABLE_PROPERTIES = Set.of("id", "firstName", "lastName");

    public ProfessionalSearchCriteria {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative.");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE + ".");
        }
        name = Strings.isBlank(name) ? null : name.trim();
        sortProperty = normalizeSortProperty(sortProperty);
        sortDirection = sortDirection == null ? SortDirection.ASC : sortDirection;
    }

    private static String normalizeSortProperty(String property) {
        String candidate = Strings.isBlank(property) ? "lastName" : property.trim();
        if (!SORTABLE_PROPERTIES.contains(candidate)) {
            throw new IllegalArgumentException(
                    "sort must be one of " + String.join(", ", SORTABLE_PROPERTIES) + ".");
        }
        return candidate;
    }

    /**
     * @return the name in lower case, ready to be used in a case insensitive match
     */
    public String namePattern() {
        return name == null ? null : "%" + name.toLowerCase(Locale.ROOT) + "%";
    }
}
