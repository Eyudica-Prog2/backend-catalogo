-- Idempotency of the Kafka consumer (contract v1, section 18.1).
--
-- Every CatalogUpdated event carries an eventId which is its functional idempotency key.
-- The consumer writes it here only AFTER the effects of the event were persisted, so a
-- delivery that arrives twice finds the row, performs no effect a second time and can
-- confirm its offset.
--
-- Old rows are pruned by the adapter (see processed_event_retention_days): a redelivered
-- event that is older than the retention window would simply be replayed, and applying the
-- pending versions is idempotent by construction, so pruning can never corrupt the copy.

CREATE TABLE processed_event (
    event_id        VARCHAR(100) PRIMARY KEY,
    catalog_version BIGINT,
    processed_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_processed_event_processed_at ON processed_event (processed_at);
