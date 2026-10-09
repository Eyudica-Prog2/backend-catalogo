package ar.edu.unlp.turnos.catalog.catalog.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Category of professionals of the catalog.
 *
 * <p>Pure domain model: no JPA, no Spring, no DTOs. {@code id} is the id assigned by the
 * catedra and is stored as received so an incremental synchronization can address it.</p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class ProfessionalCategory {

    private Long id;
    private String name;
    private String description;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
