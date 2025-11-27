#!/bin/bash

echo "Setting up Log Format Compatibility Layer..."

# Start infrastructure
echo "Starting Docker infrastructure..."
docker compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Create Kafka topics
echo "Creating Kafka topics..."
docker exec $(docker ps -qf "name=kafka") kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic raw-syslog-logs \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

docker exec $(docker ps -qf "name=kafka") kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic raw-journald-logs \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

docker exec $(docker ps -qf "name=kafka") kafka-topics --create \
    --bootstrap-server localhost:9092 \
    --topic normalized-logs \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

echo ""
echo "Infrastructure ready!"
echo "- Kafka: localhost:9092"
echo "- Redis: localhost:6379"
echo "- PostgreSQL: localhost:5432"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Build and run services with:"
echo "  mvn clean install"
echo "  java -jar syslog-adapter/target/syslog-adapter-1.0.0.jar"
echo "  java -jar journald-adapter/target/journald-adapter-1.0.0.jar"
echo "  java -jar format-normalizer/target/format-normalizer-1.0.0.jar"
echo "  java -jar api-gateway/target/api-gateway-1.0.0.jar"
echo ""
echo "Start Dashboard Server with:"
echo "  cd dashboard-server"
echo "  ./start.sh"
echo "  # Or: npm install && npm start"
echo "  # Dashboard: http://localhost:8085"
