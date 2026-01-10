#!/bin/bash

echo "=================================="
echo "Setting Up Exactly-Once Log Processing System"
echo "=================================="

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    exit 1
fi

# Check for docker compose (v2) or docker-compose (v1)
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
elif command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
else
    echo "❌ Docker Compose is not installed"
    exit 1
fi

echo "✓ Prerequisites met"

# Check for port conflicts
echo -e "\nChecking for port conflicts..."
CONFLICT_PORTS=()
for port in 6379 5432 8080 8081 8082 9090 3000 9092 9093 2181; do
    if ss -tuln 2>/dev/null | grep -q ":${port} " || netstat -tuln 2>/dev/null | grep -q ":${port} "; then
        CONFLICT_PORTS+=($port)
    fi
done

if [ ${#CONFLICT_PORTS[@]} -gt 0 ]; then
    echo "⚠️  Warning: Ports ${CONFLICT_PORTS[*]} are already in use"
    echo "   Docker services will use these ports - you may need to stop conflicting services"
fi

# Start infrastructure
echo -e "\nStarting infrastructure services..."
$DOCKER_COMPOSE_CMD up -d zookeeper kafka redis postgres prometheus grafana

echo "Waiting for services to be healthy..."
sleep 30

# Wait for Kafka to be ready
echo -e "\nWaiting for Kafka to be ready..."
MAX_RETRIES=30
RETRY_COUNT=0
KAFKA_READY=false

KAFKA_CONTAINER=$($DOCKER_COMPOSE_CMD ps -q kafka)
if [ -z "$KAFKA_CONTAINER" ]; then
    KAFKA_CONTAINER=$(docker ps --filter "name=kafka" --format "{{.Names}}" | head -1)
fi

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker exec "$KAFKA_CONTAINER" kafka-broker-api-versions --bootstrap-server localhost:9092 &>/dev/null; then
        KAFKA_READY=true
        break
    fi
    echo "  Waiting for Kafka... ($((RETRY_COUNT + 1))/$MAX_RETRIES)"
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT + 1))
done

if [ "$KAFKA_READY" = false ]; then
    echo "❌ Kafka did not become ready in time"
    exit 1
fi

echo "✓ Kafka is ready"

# Create Kafka topics
echo -e "\nCreating Kafka topics..."
if [ -n "$KAFKA_CONTAINER" ]; then
    docker exec "$KAFKA_CONTAINER" kafka-topics \
        --create --if-not-exists \
        --bootstrap-server localhost:9092 \
        --topic raw-logs \
        --partitions 3 \
        --replication-factor 1 \
        --config retention.ms=604800000 2>&1 | grep -v "already exists" || true

    docker exec "$KAFKA_CONTAINER" kafka-topics \
        --create --if-not-exists \
        --bootstrap-server localhost:9092 \
        --topic audit-logs \
        --partitions 3 \
        --replication-factor 1 2>&1 | grep -v "already exists" || true

    docker exec "$KAFKA_CONTAINER" kafka-topics \
        --create --if-not-exists \
        --bootstrap-server localhost:9092 \
        --topic processed-logs \
        --partitions 3 \
        --replication-factor 1 2>&1 | grep -v "already exists" || true
    
    echo "✓ Kafka topics created/verified"
else
    echo "❌ Could not find Kafka container"
    exit 1
fi

# Build and start applications
echo -e "\nBuilding Maven projects..."
mvn clean package -DskipTests

echo -e "\nStarting application services..."
$DOCKER_COMPOSE_CMD up -d api-gateway log-producer log-consumer

echo -e "\nWaiting for applications to start..."
sleep 20

echo "=================================="
echo "System Ready!"
echo "=================================="
echo ""
echo "Service Endpoints:"
echo "  API Gateway:    http://localhost:8080"
echo "  Log Producer:   http://localhost:8081"
echo "  Log Consumer:   http://localhost:8082"
echo "  Prometheus:     http://localhost:9090"
echo "  Grafana:        http://localhost:3000 (admin/admin)"
echo ""
echo "Quick Test:"
echo "  curl -X POST http://localhost:8080/api/logs \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"eventType\":\"INFO\",\"service\":\"test\",\"message\":\"Hello\",\"severity\":\"LOW\"}'"
echo ""
echo "Run Integration Tests:"
echo "  ./integration-tests/test-exactly-once.sh"
echo ""
echo "Run Load Test:"
echo "  ./load-test.sh"
echo "=================================="
