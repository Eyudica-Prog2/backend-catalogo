package ar.edu.um.turnos.catalog.shared.error;

/**
 * One entry of the {@code fieldErrors} array of a validation problem response.
 *
 * @param objectName validated object name
 * @param field      failing field, empty for object level errors
 * @param message    human readable message; clients branch on code, not on this text
 */
public record FieldErrorDetail(String objectName, String field, String message) {
}
