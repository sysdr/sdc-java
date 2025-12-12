#!/bin/bash

echo "=== Day 28: Quorum-Based Log Storage Setup ==="
echo

# Build and start services
echo "Building Docker images..."
docker-compose build

echo
echo "Starting services..."
docker-compose up -d

echo
echo "Waiting for services to be healthy (this may take 2-3 minutes)..."

# Wait for API Gateway to be healthy
for i in {1..60}; do
    if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo "✓ System is ready!"
        break
    fi
    echo -n "."
    sleep 3
done

echo
echo
echo "=== System Status ==="
docker-compose ps
echo

echo "=== Access Points ==="
echo "API Gateway:  http://localhost:8080"
echo "Prometheus:   http://localhost:9090"
echo "Grafana:      http://localhost:3000 (admin/admin)"
echo

echo "=== Quick Test ==="
echo "Run: ./integration-tests/test_quorum.sh"
echo "Load test: ./load-test.sh"
echo "Failure test: ./failure-test.sh"
echo

echo "=== Setup Complete ==="
