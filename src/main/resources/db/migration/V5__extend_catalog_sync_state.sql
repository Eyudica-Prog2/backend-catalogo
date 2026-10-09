-- Incremental synchronization (contract v1, sections 14 and 16).
--
-- The row of catalog_sync_state grows with the information reported by
-- GET /api/sync/status once Redis takes part in the synchronization:
--
--   last_sync_result         outcome of the last attempt (NULL before the first attempt of
--                            this version of the service);
--   current_version          version window observed in catedra:sync:current-version during
--                            the last attempt. NULL when it was never observed;
--   oldest_available_version version observed in catedra:sync:oldest-available-version.
--
-- current_version / oldest_available_version are a SNAPSHOT of the observation: the status
-- endpoint refreshes them from Redis on every read and falls back to these columns when
-- Redis is unreachable, so the operator always sees the last known window.

ALTER TABLE catalog_sync_state
    ADD COLUMN last_sync_result       VARCHAR(30),
    ADD COLUMN current_version        BIGINT,
    ADD COLUMN oldest_available_version BIGINT;

ALTER TABLE catalog_sync_state
    ADD CONSTRAINT ck_catalog_sync_state_last_sync_result
        CHECK (last_sync_result IS NULL OR last_sync_result IN (
            'SNAPSHOT_APPLIED',
            'INCREMENTAL_APPLIED',
            'ALREADY_CURRENT',
            'FAILED'));
