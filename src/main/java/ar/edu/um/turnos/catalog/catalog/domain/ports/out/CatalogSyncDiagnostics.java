package ar.edu.um.turnos.catalog.catalog.domain.ports.out;

import ar.edu.um.turnos.catalog.catalog.domain.model.CatalogSyncDiagnostic;

/**
 * Outbound port: breadcrumb written to the private namespace of this project
 * ({@code alumnos:{groupId}:diagnostics:last-sync}, contract v1, section 14.6).
 *
 * <p>Purely auxiliary: it helps an operator see what the last synchronization did without
 * opening the database. It never feeds a decision and its failure never fails a
 * synchronization.</p>
 */
public interface CatalogSyncDiagnostics {

    /**
     * @param diagnostic description of the attempt to record
     * @throws RuntimeException when the namespace cannot be written; the caller treats it
     *                          as a warning and continues
     */
    void record(CatalogSyncDiagnostic diagnostic);
}
