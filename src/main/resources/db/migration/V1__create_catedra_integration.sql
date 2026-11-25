-- Technical integration with the catedra service (single row per project).
-- Holds the NON secret values returned by GET /api/student/integration.
-- redisPassword is never stored in clear text: the adapter stores the
-- AES-256-GCM ciphertext produced by shared/crypto/AesGcmSecretCipher.
-- The technical JWT is NOT persisted at all: it lives in memory only.

CREATE TABLE catedra_integration (
    id                          BIGSERIAL PRIMARY KEY,
    group_id                    VARCHAR(100)  NOT NULL,
    redis_host                  VARCHAR(255)  NOT NULL,
    redis_port                  INTEGER       NOT NULL,
    redis_username              VARCHAR(255),
    redis_password_ciphertext   VARCHAR(1024),
    redis_read_namespace        VARCHAR(255)  NOT NULL,
    redis_write_namespace       VARCHAR(255)  NOT NULL,
    kafka_bootstrap_servers     VARCHAR(255)  NOT NULL,
    kafka_consumer_group_id     VARCHAR(255)  NOT NULL,
    kafka_catalog_topic         VARCHAR(255)  NOT NULL,
    kafka_actions_topic         VARCHAR(255)  NOT NULL,
    kafka_phone_topic           VARCHAR(255)  NOT NULL,
    provisioning_status         VARCHAR(20)   NOT NULL,
    last_refreshed_at           TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ   NOT NULL,
    updated_at                  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT ck_catedra_integration_redis_port
        CHECK (redis_port > 0 AND redis_port <= 65535),
    CONSTRAINT ck_catedra_integration_provisioning_status
        CHECK (provisioning_status IN ('PENDING', 'PROVISIONED', 'FAILED', 'REVOKED', 'UNKNOWN'))
);

-- The service keeps one integration row; this index supports the "latest row" lookup.
CREATE INDEX idx_catedra_integration_group_id ON catedra_integration (group_id);
