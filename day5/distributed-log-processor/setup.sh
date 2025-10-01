#!/bin/bash

set -e

echo "🚀 Setting up Distributed Log Processing System..."

# Start infrastructure services
echo "📦 Starting infrastructure services..."
docker-compose up -d

# Wait for services to be healthy
echo "⏳ Waiting for services to start..."
sleep 30

# Check if Kafka is ready
echo "🔍 Checking Kafka readiness..."
timeout 60 bash -c 'until docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list &> /dev/null; do sleep 2; done'

# Create Kafka topic
echo "📝 Creating Kafka topics..."
docker exec kafka kafka-topics --create --bootstrap-server localhost:9092 --topic log-events --partitions 3 --replication-factor 1 --if-not-exists

# Build and start applications
echo "🔨 Building applications..."
mvn clean package -DskipTests

# Create logs directory for application logs
mkdir -p logs

echo "🚀 Starting applications..."

# Start log-consumer first
echo "Starting log-consumer..."
cd log-consumer && mvn spring-boot:run > ../logs/consumer.log 2>&1 &
CONSUMER_PID=$!
cd ..

sleep 10

# Start log-producer
echo "Starting log-producer..."
cd log-producer && mvn spring-boot:run > ../logs/producer.log 2>&1 &
PRODUCER_PID=$!
cd ..

sleep 10

# Start api-gateway
echo "Starting api-gateway..."
cd api-gateway && mvn spring-boot:run > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
cd ..

# Save PIDs for cleanup
echo $CONSUMER_PID > logs/consumer.pid
echo $PRODUCER_PID > logs/producer.pid
echo $GATEWAY_PID > logs/gateway.pid

echo "✅ System setup complete!"
echo ""
echo "🌐 Service URLs:"
echo "  API Gateway: http://localhost:8080"
echo "  Log Producer: http://localhost:8081"
echo "  Log Consumer: http://localhost:8082"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo "  Prometheus: http://localhost:9090"
echo ""
echo "📊 To view metrics: http://localhost:3000"
echo "🔍 To send test logs: curl -X POST http://localhost:8081/api/v1/logs -H 'Content-Type: application/json' -d '{\"level\":\"INFO\",\"source\":\"test\",\"message\":\"Test message\"}'"
echo ""
echo "🛑 To stop: ./shutdown.sh"
