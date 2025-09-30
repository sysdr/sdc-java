#!/bin/bash
set -e

echo "🚀 Setting up Distributed Log Collector System..."

# Start infrastructure services
echo "📦 Starting infrastructure services..."
docker compose up -d zookeeper kafka redis postgresql prometheus grafana

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 30

# Create Kafka topics
echo "📋 Creating Kafka topics..."
docker exec kafka kafka-topics --create \
  --topic log-events \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --if-not-exists

# Create shared volume for logs
mkdir -p /tmp/logs
chmod 777 /tmp/logs

echo "✅ Infrastructure setup complete!"
echo ""
echo "🔗 Access URLs:"
echo "  • API Gateway: http://localhost:8080"
echo "  • Log Generator: http://localhost:8081"
echo "  • Log Collector: http://localhost:8082"
echo "  • Grafana: http://localhost:3000 (admin/admin)"
echo "  • Prometheus: http://localhost:9090"
echo ""
echo "📊 System Stats: http://localhost:8080/api/system/stats"
echo ""
echo "🏗️  Build and start services:"
echo "  mvn clean compile"
echo "  mvn spring-boot:run -pl log-generator &"
echo "  mvn spring-boot:run -pl log-collector &"
echo "  mvn spring-boot:run -pl api-gateway &"
