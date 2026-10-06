package ar.edu.unlp.turnos.catalog.shared.util;

/**
 * Small string helpers shared across slices. Keeps the project free of extra dependencies
 * and avoids repeating {@code null}/{@code blank} checks in every layer.
 */
public final class Strings {

    private Strings() {
    }

    /**
     * @param value value to check
     * @return true when the value is null, empty or only whitespace
     */
    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * @param value value to check
     * @return true when the value contains at least one non whitespace character
     */
    public static boolean isNotBlank(String value) {
        return !isBlank(value);
    }
}
