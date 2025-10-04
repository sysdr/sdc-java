#!/bin/bash

set -e

echo "🚀 Setting up TCP Log Server system..."

# Build the log-server module
echo "📦 Building log-server..."
cd log-server
mvn clean package -DskipTests
cd ..

# Start Docker Compose services
echo "🐳 Starting Docker Compose services..."
docker-compose up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be ready..."
sleep 30

# Check health
echo "🏥 Checking service health..."
curl -s http://localhost:8080/actuator/health | jq .

echo "✅ Setup complete!"
echo ""
echo "Services available at:"
echo "  - TCP Log Server: tcp://localhost:9090"
echo "  - REST API: http://localhost:8080"
echo "  - Health: http://localhost:8080/actuator/health"
echo "  - Metrics: http://localhost:8080/actuator/prometheus"
echo "  - Prometheus: http://localhost:9091"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "To run integration tests: ./integration-tests/test-tcp-connection.sh"
echo "To run load tests: ./load-test.sh"
