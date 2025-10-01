-- Create additional indexes for performance (these will be created after the table exists)
-- Note: These indexes will be created by the application when it starts

-- Create function for log cleanup
CREATE OR REPLACE FUNCTION cleanup_old_logs(days_to_keep INTEGER DEFAULT 30)
RETURNS INTEGER AS $$
DECLARE
    deleted_count INTEGER;
BEGIN
    DELETE FROM log_events 
    WHERE timestamp < NOW() - INTERVAL '1 day' * days_to_keep;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO loguser;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO loguser;
