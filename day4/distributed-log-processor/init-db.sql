-- Create log_events table
CREATE TABLE IF NOT EXISTS log_events (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP,
    ip_address VARCHAR(45),
    http_method VARCHAR(10),
    request_path VARCHAR(500),
    status_code INTEGER,
    response_size BIGINT,
    user_agent VARCHAR(1000),
    referer VARCHAR(500),
    response_time DOUBLE PRECISION,
    log_format VARCHAR(50),
    raw_log TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_log_events_timestamp ON log_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_log_events_ip_address ON log_events(ip_address);
CREATE INDEX IF NOT EXISTS idx_log_events_status_code ON log_events(status_code);
CREATE INDEX IF NOT EXISTS idx_log_events_created_at ON log_events(created_at);

-- Create a partition for log_events by date (optional for high volume)
-- CREATE TABLE log_events_y2024m01 PARTITION OF log_events 
-- FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
