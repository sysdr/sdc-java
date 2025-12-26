#!/bin/bash

# Setup script for Kafka Consumer System

echo "🚀 Setting up Kafka Consumer System..."

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed. Please install Docker first."
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Build Maven projects
echo "📦 Building Maven projects..."
mvn clean install -DskipTests

# Start infrastructure
echo "🐳 Starting Docker containers..."
docker-compose up -d

echo ""
echo "⏳ Waiting for services to initialize..."
echo "   This may take 2-3 minutes..."

# Wait for Kafka
echo "⏳ Waiting for Kafka..."
timeout=120
elapsed=0
while ! docker exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; do
    sleep 5
    elapsed=$((elapsed + 5))
    if [ $elapsed -ge $timeout ]; then
        echo "❌ Kafka failed to start within ${timeout} seconds"
        exit 1
    fi
    echo "   Still waiting... (${elapsed}s)"
done
echo "✅ Kafka is ready"

# Wait for consumers
echo "⏳ Waiting for consumer services..."
sleep 30

# Verify services
echo ""
echo "🔍 Verifying services..."

services=("api-gateway:8080" "log-consumer-1:8082")
for service in "${services[@]}"; do
    name=$(echo $service | cut -d: -f1)
    port=$(echo $service | cut -d: -f2)
    
    if docker exec $name curl -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo "✅ $name is healthy"
    else
        echo "⚠️ $name health check failed (might need more time)"
    fi
done

echo ""
echo "✅ Setup completed successfully!"
echo ""
echo "📊 System URLs:"
echo "   API Gateway:  http://localhost:8080"
echo "   Prometheus:   http://localhost:9090"
echo "   Grafana:      http://localhost:3000 (admin/admin)"
echo ""
echo "🧪 Run integration tests:"
echo "   ./integration-tests/test-end-to-end.sh"
echo ""
echo "🔥 Run load tests:"
echo "   ./load-test.sh <duration_seconds> <logs_per_second>"
echo "   Example: ./load-test.sh 60 1000"
echo ""
echo "📊 View consumer metrics:"
echo "   curl http://localhost:8080/api/monitoring/metrics/consumer | jq"
echo ""
echo "🛑 To stop the system:"
echo "   docker-compose down"
