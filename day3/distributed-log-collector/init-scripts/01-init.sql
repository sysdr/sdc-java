-- Create log events table for future use
CREATE TABLE IF NOT EXISTS log_events (
    id VARCHAR(255) PRIMARY KEY,
    file_path VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    file_offset BIGINT NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    hostname VARCHAR(255),
    service_name VARCHAR(255),
    content_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index for faster queries
CREATE INDEX IF NOT EXISTS idx_log_events_timestamp ON log_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_events_service ON log_events(service_name);
CREATE INDEX IF NOT EXISTS idx_log_events_hash ON log_events(content_hash);
