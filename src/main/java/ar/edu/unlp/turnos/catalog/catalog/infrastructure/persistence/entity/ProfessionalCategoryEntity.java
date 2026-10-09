package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA representation of the {@code professional_category} table.
 *
 * <p>Lives exclusively in the infrastructure layer: the domain model never imports it.
 * {@code id} is assigned by the catedra and stored as received (no generator), because a
 * future incremental synchronization addresses records by that id.</p>
 */
@Entity
@Table(name = "professional_category")
@Getter
@Setter
public class ProfessionalCategoryEntity {

    @Id
    private Long id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
