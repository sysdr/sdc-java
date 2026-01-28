#!/bin/bash

set -e

echo "🚀 Setting up Faceted Search System..."

# Build all services
echo "📦 Building services..."
mvn clean package -DskipTests

# Start infrastructure
echo "🐳 Starting Docker services..."
docker-compose up -d zookeeper kafka elasticsearch redis prometheus grafana

# Wait for infrastructure
echo "⏳ Waiting for infrastructure to be ready..."
sleep 30

# Create Kafka topics
echo "📬 Creating Kafka topics..."
KAFKA_CONTAINER=$(docker ps -qf "name=kafka")
if [ -n "$KAFKA_CONTAINER" ]; then
  sleep 10  # Wait for Kafka to be ready
  docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server localhost:9092 \
    --create --topic logs \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists || echo "Topic may already exist"
else
  echo "⚠️ Kafka container not found"
fi

# Start application services
echo "🚀 Starting application services..."
docker-compose up -d log-producer faceted-search-service aggregation-service api-gateway

# Wait for services
echo "⏳ Waiting for services to start..."
sleep 45

# Health checks
echo "🏥 Running health checks..."
curl -f http://localhost:8080/api/health || echo "⚠️ Gateway not ready"
curl -f http://localhost:8082/api/search/health || echo "⚠️ Search service not ready"

echo ""
echo "✅ Faceted Search System is ready!"
echo ""
echo "📊 Access Points:"
echo "  - API Gateway: http://localhost:8080"
echo "  - Search Service: http://localhost:8082"
echo "  - Elasticsearch: http://localhost:9200"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "🧪 Run tests:"
echo "  - Integration: ./integration-tests/test_faceted_search.sh"
echo "  - Load test: ./load-test.sh"
echo ""
echo "📖 See README.md for usage examples"
