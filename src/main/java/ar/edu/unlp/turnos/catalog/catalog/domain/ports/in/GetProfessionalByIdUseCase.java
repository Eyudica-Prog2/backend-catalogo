package ar.edu.unlp.turnos.catalog.catalog.domain.ports.in;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.ProfessionalDetails;

/**
 * Use case: read one professional of the local copy together with its weekly schedules.
 */
public interface GetProfessionalByIdUseCase {

    /**
     * @param professionalId remote id of the professional
     * @return the professional and its weekly schedules
     * @throws RuntimeException {@code 404 PROFESSIONAL_NOT_FOUND} when the local copy does
     *                          not contain that professional
     */
    ProfessionalDetails getProfessionalById(Long professionalId);
}
