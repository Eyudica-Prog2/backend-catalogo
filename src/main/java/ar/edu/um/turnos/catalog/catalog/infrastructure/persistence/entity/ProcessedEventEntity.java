package ar.edu.um.turnos.catalog.catalog.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA representation of one entry of {@code processed_event}: a {@code CatalogUpdated}
 * eventId that this service already applied (contract v1, section 18.1).
 *
 * <p>The identifier is the functional idempotency key of the message, so the primary key is
 * what turns a duplicated delivery into a no-op.</p>
 */
@Entity
@Table(name = "processed_event")
@Getter
@Setter
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "catalog_version")
    private Long catalogVersion;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now();
        }
    }
}
