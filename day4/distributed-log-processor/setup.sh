#!/bin/bash

echo "🚀 Setting up Distributed Log Processing System..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Create Kafka topics
echo "📝 Creating Kafka topics..."
docker-compose up -d zookeeper kafka

# Wait for Kafka to be ready
echo "⏳ Waiting for Kafka to be ready..."
sleep 30

# Create topics
docker exec kafka kafka-topics --create --topic raw-logs --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 || true
docker exec kafka kafka-topics --create --topic parsed-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 || true
docker exec kafka kafka-topics --create --topic parsing-dlq --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 || true

echo "✅ Kafka topics created successfully"

# Start all infrastructure services
echo "🐳 Starting all infrastructure services..."
docker-compose up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to be healthy..."
sleep 60

# Build the applications
echo "🔨 Building Spring Boot applications..."
mvn clean package -DskipTests

echo "✅ Setup completed successfully!"
echo ""
echo "🌟 System is ready! You can now:"
echo "  • Start log-producer: java -jar log-producer/target/log-producer-1.0.0.jar"
echo "  • Start log-parser: java -jar log-parser/target/log-parser-1.0.0.jar"
echo "  • Start api-gateway: java -jar api-gateway/target/api-gateway-1.0.0.jar"
echo ""
echo "🔗 Access URLs:"
echo "  • API Gateway: http://localhost:8080/api/logs"
echo "  • Log Producer: http://localhost:8081/api/logs/health"
echo "  • Log Parser: http://localhost:8082/actuator/health"
echo "  • Prometheus: http://localhost:9090"
echo "  • Grafana: http://localhost:3000 (admin/admin)"
echo ""
