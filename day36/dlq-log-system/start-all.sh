#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Starting Dead Letter Queue System ==="

# Check if services are already running
echo "Checking for running services..."
if pgrep -f "log-producer.*spring-boot:run" > /dev/null; then
    echo "WARNING: Log Producer is already running"
fi
if pgrep -f "log-consumer.*spring-boot:run" > /dev/null; then
    echo "WARNING: Log Consumer is already running"
fi
if pgrep -f "api-gateway.*spring-boot:run" > /dev/null; then
    echo "WARNING: API Gateway is already running"
fi

# Start infrastructure
echo "Starting infrastructure (Docker Compose)..."
if [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    # Stop Prometheus container if running (we'll run it on host instead)
    docker-compose stop prometheus 2>/dev/null || true
    docker-compose rm -f prometheus 2>/dev/null || true
    
    docker-compose up -d
    echo "Waiting for services to be ready..."
    sleep 25
    
    # Create Kafka topics
    echo "Creating Kafka topics..."
    docker exec dlq-log-system-kafka-1 kafka-topics --create --topic log-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    docker exec dlq-log-system-kafka-1 kafka-topics --create --topic log-events-retry --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    docker exec dlq-log-system-kafka-1 kafka-topics --create --topic log-events-dlq --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 2>/dev/null || true
    echo "Infrastructure ready!"
else
    echo "ERROR: docker-compose.yml not found in $SCRIPT_DIR"
    exit 1
fi

# Start Prometheus on host (avoids WSL2 Docker networking issues)
echo ""
echo "Starting Prometheus on host..."
if [ -f "$SCRIPT_DIR/start-prometheus.sh" ]; then
    "$SCRIPT_DIR/start-prometheus.sh"
else
    echo "WARNING: start-prometheus.sh not found, skipping Prometheus"
fi

# Build all services
echo ""
echo "Building all services..."
cd "$SCRIPT_DIR/log-producer" && mvn clean install -DskipTests > /dev/null 2>&1
cd "$SCRIPT_DIR/log-consumer" && mvn clean install -DskipTests > /dev/null 2>&1
cd "$SCRIPT_DIR/api-gateway" && mvn clean install -DskipTests > /dev/null 2>&1
echo "Build complete!"

# Start services in background
echo ""
echo "Starting services..."
cd "$SCRIPT_DIR/log-producer"
nohup mvn spring-boot:run > ../logs/producer.log 2>&1 &
PRODUCER_PID=$!
echo "Log Producer started (PID: $PRODUCER_PID) on port 8081"

sleep 5

cd "$SCRIPT_DIR/log-consumer"
nohup mvn spring-boot:run > ../logs/consumer.log 2>&1 &
CONSUMER_PID=$!
echo "Log Consumer started (PID: $CONSUMER_PID) on port 8082"

sleep 5

cd "$SCRIPT_DIR/api-gateway"
nohup mvn spring-boot:run > ../logs/gateway.log 2>&1 &
GATEWAY_PID=$!
echo "API Gateway started (PID: $GATEWAY_PID) on port 8080"

# Wait for services to be ready
echo ""
echo "Waiting for services to start..."
sleep 15

# Check if services are running
check_service() {
    local port=$1
    local name=$2
    for i in {1..30}; do
        if curl -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            echo "$name is ready on port $port"
            return 0
        fi
        sleep 1
    done
    echo "WARNING: $name may not be ready on port $port"
    return 1
}

check_service 8081 "Log Producer"
check_service 8082 "Log Consumer"
check_service 8080 "API Gateway"

echo ""
echo "=== Services Started ==="
echo "Log Producer: http://localhost:8081"
echo "Log Consumer: http://localhost:8082"
echo "API Gateway: http://localhost:8080"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "PIDs: Producer=$PRODUCER_PID, Consumer=$CONSUMER_PID, Gateway=$GATEWAY_PID"
echo "Logs: $SCRIPT_DIR/logs/"

