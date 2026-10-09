-- Local copy of the catalog: categories, professionals and weekly schedules, plus the
-- state of the synchronization. This service is the OWNER of these tables; backend-turnos
-- never reads them, it consumes the internal HTTP contract instead.
--
-- Remote ids are stored exactly as the catedra returns them (no local sequence), so a
-- future incremental synchronization can address individual records (contract v1, section 7).
-- Disabled records are kept: they are logical deletions, never physical ones (section 7).

CREATE TABLE professional_category (
    id           BIGINT       PRIMARY KEY,
    name         VARCHAR(120) NOT NULL,
    description  VARCHAR(500),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);

CREATE TABLE professional (
    id           BIGINT       PRIMARY KEY,
    category_id  BIGINT       NOT NULL,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_professional_category
        FOREIGN KEY (category_id) REFERENCES professional_category (id)
);

CREATE INDEX idx_professional_category_id ON professional (category_id);
CREATE INDEX idx_professional_enabled ON professional (enabled);

CREATE TABLE weekly_schedule (
    id                    BIGINT      PRIMARY KEY,
    professional_id       BIGINT      NOT NULL,
    day_of_week           VARCHAR(10) NOT NULL,
    start_time            TIME        NOT NULL,
    end_time              TIME        NOT NULL,
    slot_duration_minutes INTEGER     NOT NULL,
    enabled               BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_weekly_schedule_professional
        FOREIGN KEY (professional_id) REFERENCES professional (id) ON DELETE CASCADE,
    CONSTRAINT ck_weekly_schedule_day
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_weekly_schedule_range
        CHECK (start_time < end_time),
    CONSTRAINT ck_weekly_schedule_slot_duration
        CHECK (slot_duration_minutes > 0)
);

CREATE INDEX idx_weekly_schedule_professional_id ON weekly_schedule (professional_id);
CREATE INDEX idx_weekly_schedule_day ON weekly_schedule (day_of_week);

-- Single row: the state of the full synchronization.
--
--   snapshot_version : last version received from the catedra (attempted);
--   applied_version  : version of the data currently stored in the three tables above.
--
-- applied_version only advances when the three collections were applied inside the same
-- transaction (contract v1, section 7). A failed attempt advances snapshot_version only,
-- which makes the gap visible through GET /api/sync/status together with last_error.
CREATE TABLE catalog_sync_state (
    id               BIGSERIAL   PRIMARY KEY,
    snapshot_version BIGINT      NOT NULL DEFAULT 0,
    applied_version  BIGINT      NOT NULL DEFAULT 0,
    last_sync_at     TIMESTAMPTZ,
    last_error       VARCHAR(1000),
    last_error_at    TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,

    CONSTRAINT ck_catalog_sync_state_single_row CHECK (id = 1)
);
