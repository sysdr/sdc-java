#!/bin/bash

echo "🚀 Starting Kafka Consumer Log Processing System..."

# Start infrastructure
echo "📦 Starting Docker containers..."
docker compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 30

# Create Kafka topics
echo "📝 Creating Kafka topics..."
sleep 10  # Wait for Kafka to be fully ready
KAFKA_CONTAINER=$(docker ps --format "{{.Names}}" | grep kafka | head -1)
if [ -n "$KAFKA_CONTAINER" ]; then
  docker exec $KAFKA_CONTAINER kafka-topics \
    --create --topic application-logs \
    --bootstrap-server localhost:9092 \
    --partitions 12 \
    --replication-factor 1 \
    --if-not-exists 2>/dev/null || echo "Topic application-logs may already exist"
  
  docker exec $KAFKA_CONTAINER kafka-topics \
    --create --topic logs-dlq \
    --bootstrap-server localhost:9092 \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists 2>/dev/null || echo "Topic logs-dlq may already exist"
else
  echo "⚠️  Kafka container not found, skipping topic creation"
fi

echo "✅ Infrastructure ready!"
echo ""
echo "📊 Access Points:"
echo "  - API Gateway:  http://localhost:8080"
echo "  - Producer API: http://localhost:8081"
echo "  - Consumer API: http://localhost:8082"
echo "  - Prometheus:   http://localhost:9090"
echo "  - Grafana:      http://localhost:3000 (admin/admin)"
echo ""
echo "🔧 Build and run services:"
echo "  mvn clean package"
echo "  java -jar log-producer/target/log-producer-1.0.0.jar &"
echo "  java -jar log-consumer/target/log-consumer-1.0.0.jar &"
echo "  java -jar api-gateway/target/api-gateway-1.0.0.jar &"
