#!/bin/bash

set -e

echo "Starting Avro Log Processing System..."

# Start infrastructure
echo "Starting Docker infrastructure..."
docker compose up -d

# Wait for Schema Registry
echo "Waiting for Schema Registry to be ready..."
until curl -s http://localhost:8085/subjects > /dev/null 2>&1; do
    echo "Schema Registry not ready yet..."
    sleep 5
done
echo "Schema Registry is ready!"

# Create Kafka topic
echo "Creating Kafka topic..."
docker exec kafka kafka-topics --create \
    --if-not-exists \
    --topic avro-log-events \
    --bootstrap-server kafka:29092 \
    --partitions 3 \
    --replication-factor 1

echo "Infrastructure is ready!"
echo ""
echo "Next steps:"
echo "1. Build the project: mvn clean install"
echo "2. Start the services:"
echo "   - Producer: java -jar log-producer/target/log-producer-1.0.0.jar"
echo "   - Consumer: java -jar log-consumer/target/log-consumer-1.0.0.jar"
echo "   - Gateway:  java -jar api-gateway/target/api-gateway-1.0.0.jar"
echo ""
echo "Access points:"
echo "  - API Gateway:     http://localhost:8080"
echo "  - Schema Registry: http://localhost:8085"
echo "  - Prometheus:      http://localhost:9090"
echo "  - Grafana:         http://localhost:3000 (admin/admin)"
