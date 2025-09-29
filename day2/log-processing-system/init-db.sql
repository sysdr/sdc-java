-- Initialize log processing database

-- Create log events table if not exists
CREATE TABLE IF NOT EXISTS log_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    level VARCHAR(10) NOT NULL,
    source VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    trace_id VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create metadata table for key-value pairs
CREATE TABLE IF NOT EXISTS log_event_metadata (
    log_event_id UUID NOT NULL,
    key VARCHAR(100) NOT NULL,
    value TEXT,
    FOREIGN KEY (log_event_id) REFERENCES log_events(id) ON DELETE CASCADE,
    PRIMARY KEY (log_event_id, key)
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_log_events_timestamp ON log_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_events_source ON log_events(source);
CREATE INDEX IF NOT EXISTS idx_log_events_level ON log_events(level);
CREATE INDEX IF NOT EXISTS idx_log_events_correlation_id ON log_events(correlation_id);

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO loguser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO loguser;
