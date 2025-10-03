-- Create indexes for optimal query performance
CREATE INDEX IF NOT EXISTS idx_log_timestamp_level ON log_entries(timestamp DESC, log_level);
CREATE INDEX IF NOT EXISTS idx_log_source ON log_entries(source);
CREATE INDEX IF NOT EXISTS idx_log_errors ON log_entries(timestamp DESC) WHERE log_level = 'ERROR';

-- Create a view for recent logs (last 24 hours)
CREATE OR REPLACE VIEW recent_logs AS
SELECT * FROM log_entries 
WHERE timestamp > NOW() - INTERVAL '24 hours'
ORDER BY timestamp DESC;
