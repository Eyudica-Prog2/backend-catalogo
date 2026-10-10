package ar.edu.um.turnos.catalog.catedra.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * JPA representation of the {@code catedra_integration} table.
 *
 * <p>It lives exclusively in the infrastructure layer: the domain model never imports it.
 * {@code redisPasswordCiphertext} holds the AES-GCM ciphertext produced by the adapter, never
 * the clear text password.</p>
 */
@Entity
@Table(name = "catedra_integration")
@Getter
@Setter
public class CatedraIntegrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false, length = 100)
    private String groupId;

    @Column(name = "redis_host", nullable = false, length = 255)
    private String redisHost;

    @Column(name = "redis_port", nullable = false)
    private int redisPort;

    @Column(name = "redis_username", length = 255)
    private String redisUsername;

    @Column(name = "redis_password_ciphertext", length = 1024)
    private String redisPasswordCiphertext;

    @Column(name = "redis_read_namespace", nullable = false, length = 255)
    private String redisReadNamespace;

    @Column(name = "redis_write_namespace", nullable = false, length = 255)
    private String redisWriteNamespace;

    @Column(name = "kafka_bootstrap_servers", nullable = false, length = 255)
    private String kafkaBootstrapServers;

    @Column(name = "kafka_consumer_group_id", nullable = false, length = 255)
    private String kafkaConsumerGroupId;

    @Column(name = "kafka_catalog_topic", nullable = false, length = 255)
    private String kafkaCatalogTopic;

    @Column(name = "kafka_actions_topic", nullable = false, length = 255)
    private String kafkaActionsTopic;

    @Column(name = "kafka_phone_topic", nullable = false, length = 255)
    private String kafkaPhoneTopic;

    @Column(name = "provisioning_status", nullable = false, length = 20)
    private String provisioningStatus;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

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
