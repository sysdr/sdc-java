-- Initial schema for log_events table.
-- Index strategy: (source, timestamp) covers the dominant query pattern.
-- We do NOT index on 'level' alone — cardinality is too low (5 values).
-- If we need level queries at scale, use a materialized view or partition table.

CREATE TABLE IF NOT EXISTS log_events (
    event_id        VARCHAR(36)  PRIMARY KEY,
    source          VARCHAR(128) NOT NULL,
    level           VARCHAR(10)  NOT NULL,
    message         TEXT         NOT NULL,
    timestamp       TIMESTAMPTZ  NOT NULL,
    correlation_id  VARCHAR(36)
);

CREATE INDEX IF NOT EXISTS idx_source_timestamp
    ON log_events (source, timestamp DESC);

CREATE INDEX IF NOT EXISTS idx_level_timestamp
    ON log_events (level, timestamp DESC);

-- Partition hint for future: if log_events grows beyond 100M rows,
-- consider range-partitioning on timestamp (monthly).
COMMENT ON TABLE log_events IS 'Persistent store for ingested log events. Partition candidate at scale.';
