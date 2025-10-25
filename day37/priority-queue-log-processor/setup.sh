#!/bin/bash

echo "🚀 Starting Priority Queue Log Processing System..."

# Create Kafka topics with specific configurations
echo "📝 Creating Kafka topics..."
docker-compose up -d zookeeper kafka
sleep 10

docker exec -it $(docker ps -q -f name=kafka) kafka-topics --create \
  --topic critical-logs --bootstrap-server localhost:9092 \
  --partitions 2 --replication-factor 1 \
  --config retention.ms=604800000

docker exec -it $(docker ps -q -f name=kafka) kafka-topics --create \
  --topic high-logs --bootstrap-server localhost:9092 \
  --partitions 4 --replication-factor 1 \
  --config retention.ms=259200000

docker exec -it $(docker ps -q -f name=kafka) kafka-topics --create \
  --topic normal-logs --bootstrap-server localhost:9092 \
  --partitions 8 --replication-factor 1 \
  --config retention.ms=86400000

docker exec -it $(docker ps -q -f name=kafka) kafka-topics --create \
  --topic low-logs --bootstrap-server localhost:9092 \
  --partitions 16 --replication-factor 1 \
  --config retention.ms=43200000

echo "✅ Kafka topics created"

# Start all services
echo "🐳 Starting all services..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 30

echo "✅ System is ready!"
echo ""
echo "📊 Access Points:"
echo "  - API Gateway: http://localhost:8080/api/stats"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Producer: http://localhost:8081/actuator/health"
echo ""
echo "📈 Monitor priority queue metrics:"
echo "  - Critical consumer latency: http://localhost:8082/actuator/metrics"
echo "  - Normal consumer latency: http://localhost:8083/actuator/metrics"
