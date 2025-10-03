#!/bin/bash

echo "Setting up Distributed Log Processing System..."

# Start infrastructure services
echo "Starting infrastructure services..."
docker-compose up -d zookeeper kafka postgres redis prometheus grafana

# Wait for services to be healthy
echo "Waiting for services to be ready..."
sleep 30

# Create Kafka topic
echo "Creating Kafka topic..."
docker exec kafka kafka-topics --create \
  --topic log-events \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 \
  --partitions 3 \
  --if-not-exists

echo "Infrastructure setup complete!"
echo ""
echo "Services available at:"
echo "- API Gateway: http://localhost:8080"
echo "- Log Producer: http://localhost:8081"
echo "- Log Consumer: http://localhost:8082"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "To build and run the applications:"
echo "1. mvn clean install"
echo "2. java -jar log-producer/target/log-producer-1.0.0.jar"
echo "3. java -jar log-consumer/target/log-consumer-1.0.0.jar"
echo "4. java -jar api-gateway/target/api-gateway-1.0.0.jar"
