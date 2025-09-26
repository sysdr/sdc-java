#!/bin/bash

set -e

echo "🚀 Setting up Distributed Log Processing System..."

# Check prerequisites
command -v docker >/dev/null 2>&1 || { echo "❌ Docker is required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "❌ Docker Compose is required but not installed. Aborting." >&2; exit 1; }

echo "📦 Starting infrastructure services..."
docker-compose up -d

echo "⏳ Waiting for services to become healthy..."

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
until docker exec postgres pg_isready -U loguser -d logprocessor; do
    echo "PostgreSQL is unavailable - sleeping"
    sleep 2
done
echo "✅ PostgreSQL is ready"

# Wait for Kafka
echo "Waiting for Kafka..."
until docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
    echo "Kafka is unavailable - sleeping"
    sleep 2
done
echo "✅ Kafka is ready"

# Create Kafka topic
echo "Creating Kafka topic..."
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic log-events --partitions 3 --replication-factor 1 --if-not-exists
echo "✅ Kafka topic created"

# Wait for Redis
echo "Waiting for Redis..."
until docker exec redis redis-cli ping; do
    echo "Redis is unavailable - sleeping"
    sleep 1
done
echo "✅ Redis is ready"

echo "🎉 All infrastructure services are ready!"
echo ""
echo "Next steps:"
echo "1. Build the applications: mvn clean package"
echo "2. Start the services:"
echo "   - Log Producer: cd log-producer && mvn spring-boot:run"
echo "   - Log Consumer: cd log-consumer && mvn spring-boot:run" 
echo "   - API Gateway: cd api-gateway && mvn spring-boot:run"
echo "3. Run integration tests: ./integration-tests/run-tests.sh"
echo "4. Access monitoring:"
echo "   - Grafana: http://localhost:3000 (admin/admin)"
echo "   - Prometheus: http://localhost:9090"
echo ""
echo "🔗 Service endpoints:"
echo "   - Producer API: http://localhost:8081/producer/api/logs"
echo "   - Gateway API: http://localhost:8080/gateway/api/query"
