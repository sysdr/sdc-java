#!/bin/bash

set -e

echo "Setting up Log Routing System..."

# Start infrastructure
echo "Starting Docker containers..."
docker-compose up -d

echo "Waiting for Kafka to be ready..."
sleep 20

# Create Kafka topics
echo "Creating Kafka topics..."
KAFKA_CONTAINER=$(docker ps -qf "name=kafka")
if [ -z "$KAFKA_CONTAINER" ]; then
    echo "Error: Kafka container not found"
    exit 1
fi

docker exec $KAFKA_CONTAINER kafka-topics \
  --create --if-not-exists --topic logs-security \
  --bootstrap-server localhost:9092 --partitions 16 --replication-factor 1 || true

docker exec $KAFKA_CONTAINER kafka-topics \
  --create --if-not-exists --topic logs-performance \
  --bootstrap-server localhost:9092 --partitions 12 --replication-factor 1 || true

docker exec $KAFKA_CONTAINER kafka-topics \
  --create --if-not-exists --topic logs-application \
  --bootstrap-server localhost:9092 --partitions 8 --replication-factor 1 || true

docker exec $KAFKA_CONTAINER kafka-topics \
  --create --if-not-exists --topic logs-system \
  --bootstrap-server localhost:9092 --partitions 4 --replication-factor 1 || true

docker exec $KAFKA_CONTAINER kafka-topics \
  --create --if-not-exists --topic logs-critical \
  --bootstrap-server localhost:9092 --partitions 8 --replication-factor 1 || true

docker exec $KAFKA_CONTAINER kafka-topics \
  --create --if-not-exists --topic logs-default \
  --bootstrap-server localhost:9092 --partitions 4 --replication-factor 1 || true

echo "Infrastructure setup complete!"
echo ""
echo "Next steps:"
echo "1. Build all services: mvn clean install"
echo "2. Start routing service: cd routing-service && mvn spring-boot:run"
echo "3. Start consumers in separate terminals:"
echo "   - cd security-consumer && mvn spring-boot:run"
echo "   - cd performance-consumer && mvn spring-boot:run"
echo "   - cd application-consumer && mvn spring-boot:run"
echo "   - cd system-consumer && mvn spring-boot:run"
echo "4. Start log producer: cd log-producer && mvn spring-boot:run"
echo "5. Run integration tests: ./integration-tests/test-routing.sh"
echo "6. Run load tests: ./load-test.sh"
echo ""
echo "Monitoring:"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
