package ar.edu.unlp.turnos.catalog.catalog.infrastructure.persistence.entity;

import ar.edu.unlp.turnos.catalog.catalog.domain.model.SyncResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA representation of the single row of {@code catalog_sync_state}.
 *
 * <p>{@code snapshotVersion} is the last version received from the catedra (attempted) and
 * {@code appliedVersion} the version of the data currently stored. They only match after a
 * successful application of the three collections.</p>
 *
 * <p>{@code currentVersion}/{@code oldestAvailableVersion} are the last version window
 * observed in Redis, stored as a fallback for {@code GET /api/sync/status}.</p>
 */
@Entity
@Table(name = "catalog_sync_state")
@Getter
@Setter
public class CatalogSyncStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_version", nullable = false)
    private long snapshotVersion;

    @Column(name = "applied_version", nullable = false)
    private long appliedVersion;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "last_error_at")
    private Instant lastErrorAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_result", length = 30)
    private SyncResult lastSyncResult;

    @Column(name = "current_version")
    private Long currentVersion;

    @Column(name = "oldest_available_version")
    private Long oldestAvailableVersion;

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
