-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create log_events table
CREATE TABLE IF NOT EXISTS log_events (
    id VARCHAR(36) PRIMARY KEY,
    level VARCHAR(10) NOT NULL,
    message TEXT NOT NULL,
    source VARCHAR(100) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

-- Create indexes for optimal query performance
CREATE INDEX IF NOT EXISTS idx_log_timestamp ON log_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_level ON log_events(level);
CREATE INDEX IF NOT EXISTS idx_log_source ON log_events(source);
CREATE INDEX IF NOT EXISTS idx_log_timestamp_level ON log_events(timestamp, level);
CREATE INDEX IF NOT EXISTS idx_log_metadata_gin ON log_events USING gin(metadata);

-- Create a sample view for analytics
CREATE OR REPLACE VIEW log_analytics AS
SELECT 
    DATE_TRUNC('hour', timestamp) as hour,
    level,
    source,
    COUNT(*) as log_count,
    AVG(EXTRACT(EPOCH FROM (created_at - timestamp))) as avg_processing_delay
FROM log_events 
GROUP BY DATE_TRUNC('hour', timestamp), level, source
ORDER BY hour DESC;
