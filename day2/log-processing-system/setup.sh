#!/bin/bash

echo "🚀 Setting up log processing system..."

# Start infrastructure services
echo "🐳 Starting Docker services..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to start..."
sleep 45

# Build and start applications
echo "🔨 Building Java applications..."
mvn clean package -DskipTests

# Create Kafka topics
echo "📝 Creating Kafka topics..."
docker exec kafka kafka-topics --create --topic log-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 || echo "Topic may already exist"

# Start Log Generator
echo "🎯 Starting Log Generator..."
java -jar log-generator/target/log-generator-1.0.0.jar &
LOG_GENERATOR_PID=$!

# Wait for Log Generator to start
sleep 20

# Start API Gateway
echo "🚪 Starting API Gateway..."
java -jar api-gateway/target/api-gateway-1.0.0.jar &
API_GATEWAY_PID=$!

# Save PIDs for cleanup
echo $LOG_GENERATOR_PID > log-generator.pid
echo $API_GATEWAY_PID > api-gateway.pid

echo "✅ Setup complete!"
echo ""
echo "🌐 Available endpoints:"
echo "  - API Gateway: http://localhost:8090"
echo "  - Log Generator: http://localhost:8080"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Zipkin: http://localhost:9411"
echo ""
echo "🧪 Run integration tests: ./integration-tests/system-test.sh"
echo "🔥 Run load tests: ./load-test.sh"
echo ""
echo "🛑 To stop services: ./stop.sh"
