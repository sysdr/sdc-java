#!/bin/bash

set -e

echo "=========================================="
echo "Setting Up Windowed Log Analytics System"
echo "=========================================="

# Start infrastructure
echo "Starting infrastructure services..."
docker-compose up -d zookeeper kafka postgres redis prometheus grafana

# Wait for Kafka to be ready
echo "Waiting for Kafka to be ready..."
sleep 30

# Build and start application services
echo "Building and starting application services..."
docker-compose up -d --build log-producer window-processor api-gateway

echo "=========================================="
echo "System started successfully!"
echo "=========================================="
echo ""
echo "Services:"
echo "  - API Gateway:       http://localhost:8080"
echo "  - Log Producer:      http://localhost:8081"
echo "  - Window Processor:  http://localhost:8082"
echo "  - Prometheus:        http://localhost:9090"
echo "  - Grafana:          http://localhost:3000 (admin/admin)"
echo ""
echo "Wait 2-3 minutes for windows to populate, then run:"
echo "  ./integration-tests/test_windows.sh"
echo "  ./load-test.sh"
echo ""
