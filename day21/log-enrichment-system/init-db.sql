-- Create host_metadata table
CREATE TABLE IF NOT EXISTS host_metadata (
    id SERIAL PRIMARY KEY,
    hostname VARCHAR(255) UNIQUE NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    datacenter VARCHAR(100),
    environment VARCHAR(50),
    cost_center VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create service_metadata table
CREATE TABLE IF NOT EXISTS service_metadata (
    id SERIAL PRIMARY KEY,
    service_name VARCHAR(255) UNIQUE NOT NULL,
    version VARCHAR(50),
    deployment_id VARCHAR(255),
    team VARCHAR(100),
    cost_center VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert sample host metadata
INSERT INTO host_metadata (hostname, ip_address, datacenter, environment, cost_center) VALUES
('web-server-01', '192.168.1.100', 'us-east-1', 'production', 'engineering'),
('web-server-02', '192.168.1.101', 'us-east-1', 'production', 'engineering'),
('api-server-01', '192.168.1.200', 'us-west-2', 'production', 'platform'),
('api-server-02', '192.168.1.201', 'us-west-2', 'production', 'platform'),
('staging-01', '192.168.2.100', 'us-east-1', 'staging', 'engineering'),
('dev-01', '192.168.3.100', 'us-east-1', 'development', 'engineering')
ON CONFLICT (hostname) DO NOTHING;

-- Insert sample service metadata
INSERT INTO service_metadata (service_name, version, deployment_id, team, cost_center) VALUES
('payment-service', 'v2.5.0', 'deploy-12345', 'payments', 'finance'),
('user-service', 'v1.8.3', 'deploy-12346', 'identity', 'engineering'),
('order-service', 'v3.0.1', 'deploy-12347', 'commerce', 'sales'),
('notification-service', 'v1.2.0', 'deploy-12348', 'messaging', 'marketing')
ON CONFLICT (service_name) DO NOTHING;
