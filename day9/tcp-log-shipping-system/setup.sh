#!/bin/bash

set -e

echo "🚀 Setting up TCP Log Shipping System"

echo "1. Building Docker images..."
docker compose build

echo "2. Starting infrastructure services..."
docker compose up -d zookeeper kafka postgres redis prometheus grafana

echo "3. Waiting for services to be ready..."
sleep 30

echo "4. Starting application services..."
docker compose up -d log-receiver log-consumer log-producer api-gateway

echo "5. Waiting for applications to be ready..."
sleep 20

echo ""
echo "✅ System is ready!"
echo ""
echo "Service URLs:"
echo "  - API Gateway:   http://localhost:8080"
echo "  - Log Producer:  http://localhost:8081"
echo "  - Log Receiver:  http://localhost:8082"
echo "  - Log Consumer:  http://localhost:8083"
echo "  - Prometheus:    http://localhost:9090"
echo "  - Grafana:       http://localhost:3000 (admin/admin)"
echo ""
echo "Run integration tests: ./integration-tests/test-system.sh"
echo "Run load tests:        ./load-test.sh"
