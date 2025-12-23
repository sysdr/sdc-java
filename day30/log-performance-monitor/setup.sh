#!/bin/bash

echo "=== Day 30: Performance Monitoring System Setup ==="

# Start infrastructure
echo "Starting infrastructure services..."
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Build all modules
echo "Building Maven modules..."
mvn clean package -DskipTests

echo ""
echo "=== Setup Complete ==="
echo ""
echo "Services:"
echo "  Performance Monitor: http://localhost:8080"
echo "  Load Generator: http://localhost:8081"
echo "  Metrics Analyzer: http://localhost:8082"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "  1. Start services: cd performance-monitor && mvn spring-boot:run"
echo "  2. Run load tests: ./load-test.sh"
echo "  3. View metrics: http://localhost:3000"
