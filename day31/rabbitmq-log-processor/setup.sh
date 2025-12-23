#!/bin/bash

echo "🚀 Setting up RabbitMQ Log Processing System..."

# Check prerequisites
command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose is required but not installed. Aborting." >&2; exit 1; }

# Start infrastructure first
echo "📦 Starting infrastructure services..."
docker-compose up -d rabbitmq postgres redis prometheus grafana

echo "⏳ Waiting for services to be healthy (60 seconds)..."
sleep 60

# Build and start application services
echo "🏗️  Building application services..."
docker-compose build log-producer log-consumer api-gateway

echo "🚀 Starting application services..."
docker-compose up -d log-producer log-consumer api-gateway

echo "⏳ Waiting for applications to start (30 seconds)..."
sleep 30

# Verify services
echo "🔍 Verifying service health..."
curl -s http://localhost:8080/api/v1/health || echo "⚠️  Gateway not responding"
curl -s http://localhost:8081/api/v1/logs/health || echo "⚠️  Producer not responding"

echo ""
echo "✅ Setup complete!"
echo ""
echo "📊 Service URLs:"
echo "  API Gateway: http://localhost:8080"
echo "  Log Producer: http://localhost:8081"
echo "  Log Consumer: http://localhost:8082"
echo "  RabbitMQ Management: http://localhost:15672 (admin/admin123)"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo "  Prometheus: http://localhost:9090"
echo ""
echo "🧪 Run integration tests: ./integration-tests/test-flow.sh"
echo "🔥 Run load tests: ./load-test.sh"
echo ""
echo "📚 View logs: docker-compose logs -f [service-name]"
echo "🛑 Stop system: docker-compose down"
