package ar.edu.unlp.turnos.catalog.catalog.domain.model;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Professional of the catalog, belonging to exactly one category.
 *
 * <p>Pure domain model. {@code id} and {@code categoryId} are the ids assigned by the
 * catedra; the local copy never creates ids of its own.</p>
 */
@Getter
@Setter
@Builder(toBuilder = true)
public class Professional {

    private Long id;
    private Long categoryId;
    private String firstName;
    private String lastName;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
