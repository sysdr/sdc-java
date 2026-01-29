#!/bin/bash

echo "Starting Real-Time Log Indexing System..."

# Start infrastructure
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Create Kafka topic
KAFKA_CONTAINER=$(docker ps -q -f name=kafka)
if [ -n "$KAFKA_CONTAINER" ]; then
    docker exec $KAFKA_CONTAINER kafka-topics \
      --create --topic raw-logs \
      --bootstrap-server localhost:9092 \
      --partitions 3 \
      --replication-factor 1 \
      --if-not-exists || echo "Topic may already exist"
else
    echo "Warning: Kafka container not found"
fi

echo "Infrastructure ready!"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Now run: mvn clean install && java -jar log-producer/target/log-producer-1.0.0.jar"
