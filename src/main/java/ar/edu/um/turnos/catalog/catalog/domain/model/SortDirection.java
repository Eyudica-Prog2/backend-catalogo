package ar.edu.um.turnos.catalog.catalog.domain.model;

import java.util.Locale;

/**
 * Direction of an ordered result.
 *
 * <p>Kept in the domain so the search criteria never depends on
 * {@code org.springframework.data.domain.Sort}.</p>
 */
public enum SortDirection {

    ASC,
    DESC;

    /**
     * @param value raw value sent by the client ({@code asc} or {@code desc}, any case)
     * @return the matching direction, {@link #ASC} when the value is empty
     * @throws IllegalArgumentException when the value is not a known direction
     */
    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return ASC;
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
