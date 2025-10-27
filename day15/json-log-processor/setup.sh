#!/bin/bash

echo "🚀 Setting up JSON Log Processing System"
echo "========================================"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

echo "✓ Docker and Docker Compose are installed"
echo ""

# Stop and remove existing containers
echo "🧹 Cleaning up existing containers..."
docker compose down -v

echo ""
echo "🏗️ Building and starting services..."
docker compose up -d --build

echo ""
echo "⏳ Waiting for services to be ready..."

# Wait for services to be healthy
max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do
    if docker compose ps | grep -q "healthy"; then
        echo "✓ Core infrastructure is healthy"
        break
    fi
    attempt=$((attempt + 1))
    echo "  Attempt $attempt/$max_attempts - waiting for services..."
    sleep 5
done

# Wait additional time for application services
echo "  Waiting for application services to start..."
sleep 20

echo ""
echo "============================================"
echo "✅ System is ready!"
echo ""
echo "Service URLs:"
echo "  - API Gateway:  http://localhost:8080"
echo "  - Log Producer: http://localhost:8081"
echo "  - Log Consumer: http://localhost:8082"
echo "  - Prometheus:   http://localhost:9090"
echo "  - Grafana:      http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "  1. Run integration tests: ./integration-tests/test_system.sh"
echo "  2. Run load tests: ./load-test.sh"
echo "  3. View logs: docker compose logs -f [service-name]"
echo "  4. Check metrics: Open Grafana at http://localhost:3000"
echo ""
echo "To stop the system: docker compose down"
echo "============================================"
