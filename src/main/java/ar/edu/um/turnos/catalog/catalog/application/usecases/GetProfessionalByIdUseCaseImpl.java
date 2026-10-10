package ar.edu.um.turnos.catalog.catalog.application.usecases;

import ar.edu.um.turnos.catalog.catalog.application.exception.CatalogException;
import ar.edu.um.turnos.catalog.catalog.domain.model.ProfessionalDetails;
import ar.edu.um.turnos.catalog.catalog.domain.ports.in.GetProfessionalByIdUseCase;
import ar.edu.um.turnos.catalog.catalog.domain.ports.out.CatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Reads one professional with its weekly schedules from the local copy.
 *
 * <p>The empty case is translated here into {@code 404 PROFESSIONAL_NOT_FOUND}, so no
 * controller repeats the conversion (anti-pattern 1).</p>
 */
@Component
@RequiredArgsConstructor
public class GetProfessionalByIdUseCaseImpl implements GetProfessionalByIdUseCase {

    private final CatalogRepository catalogRepository;

    @Override
    public ProfessionalDetails getProfessionalById(Long professionalId) {
        return catalogRepository.findProfessionalById(professionalId)
                .orElseThrow(CatalogException::professionalNotFound);
    }
}
