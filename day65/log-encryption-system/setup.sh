#!/bin/bash

set -e

echo "🚀 Setting up Log Encryption System..."

# Check dependencies
command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed."; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose is required but not installed."; exit 1; }

# Start infrastructure
echo "📦 Starting infrastructure services..."
docker-compose up -d kafka redis postgres prometheus grafana

# Wait for infrastructure
echo "⏳ Waiting for infrastructure to be ready..."
sleep 20

# Build and start application services
echo "🔨 Building and starting application services..."
docker-compose up -d --build encryption-service log-producer log-consumer query-service

echo "✅ All services started!"
echo ""
echo "📡 Service endpoints:"
echo "  - Log Producer:      http://localhost:8080"
echo "  - Encryption Service: http://localhost:8081"
echo "  - Log Consumer:      http://localhost:8082"
echo "  - Query Service:     http://localhost:8083"
echo "  - Prometheus:        http://localhost:9090"
echo "  - Grafana:           http://localhost:3000 (admin/admin)"
echo ""
echo "🧪 Run integration tests: ./integration-tests/test-encryption-flow.sh"
echo "🚀 Run load test: ./load-test.sh"
