#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Starting Log Routing System..."

# Check if services are already running
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 ; then
        return 0
    else
        return 1
    fi
}

# Check for duplicate services
echo "Checking for running services..."
PORTS=(8080 8081 8082 8083 8084 8085)
SERVICES=("log-producer" "routing-service" "security-consumer" "performance-consumer" "application-consumer" "system-consumer")

for i in "${!PORTS[@]}"; do
    if check_port "${PORTS[$i]}"; then
        echo "WARNING: Port ${PORTS[$i]} (${SERVICES[$i]}) is already in use!"
        echo "Checking process..."
        lsof -i :${PORTS[$i]} || true
    fi
done

# Build all services first
echo ""
echo "Building all services..."
mvn clean install -DskipTests

# Start infrastructure if not running
if ! docker ps | grep -q kafka; then
    echo ""
    echo "Starting Docker infrastructure..."
    docker-compose up -d
    
    echo "Waiting for Kafka to be ready..."
    sleep 20
    
    # Create Kafka topics
    KAFKA_CONTAINER=$(docker ps -qf "name=kafka")
    if [ -z "$KAFKA_CONTAINER" ]; then
        echo "Error: Kafka container not found"
        exit 1
    fi
    
    echo "Creating Kafka topics..."
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
else
    echo "Docker infrastructure already running"
fi

# Create logs directory before starting services
mkdir -p "$SCRIPT_DIR/logs"

# Start services in background
echo ""
echo "Starting services..."

# Start routing service first (others depend on it)
echo "Starting routing-service on port 8081..."
cd "$SCRIPT_DIR/routing-service"
nohup mvn spring-boot:run > ../logs/routing-service.log 2>&1 &
ROUTING_PID=$!
echo "Routing service started with PID: $ROUTING_PID"
sleep 5

# Start consumers
echo "Starting security-consumer on port 8082..."
cd "$SCRIPT_DIR/security-consumer"
nohup mvn spring-boot:run > ../logs/security-consumer.log 2>&1 &
SECURITY_PID=$!
echo "Security consumer started with PID: $SECURITY_PID"
sleep 3

echo "Starting performance-consumer on port 8083..."
cd "$SCRIPT_DIR/performance-consumer"
nohup mvn spring-boot:run > ../logs/performance-consumer.log 2>&1 &
PERFORMANCE_PID=$!
echo "Performance consumer started with PID: $PERFORMANCE_PID"
sleep 3

echo "Starting application-consumer on port 8084..."
cd "$SCRIPT_DIR/application-consumer"
nohup mvn spring-boot:run > ../logs/application-consumer.log 2>&1 &
APPLICATION_PID=$!
echo "Application consumer started with PID: $APPLICATION_PID"
sleep 3

echo "Starting system-consumer on port 8085..."
cd "$SCRIPT_DIR/system-consumer"
nohup mvn spring-boot:run > ../logs/system-consumer.log 2>&1 &
SYSTEM_PID=$!
echo "System consumer started with PID: $SYSTEM_PID"
sleep 3

# Start log producer last
echo "Starting log-producer on port 8080..."
cd "$SCRIPT_DIR/log-producer"
nohup mvn spring-boot:run > ../logs/log-producer.log 2>&1 &
PRODUCER_PID=$!
echo "Log producer started with PID: $PRODUCER_PID"

# Save PIDs to file
echo "$ROUTING_PID" > "$SCRIPT_DIR/logs/routing-service.pid"
echo "$SECURITY_PID" > "$SCRIPT_DIR/logs/security-consumer.pid"
echo "$PERFORMANCE_PID" > "$SCRIPT_DIR/logs/performance-consumer.pid"
echo "$APPLICATION_PID" > "$SCRIPT_DIR/logs/application-consumer.pid"
echo "$SYSTEM_PID" > "$SCRIPT_DIR/logs/system-consumer.pid"
echo "$PRODUCER_PID" > "$SCRIPT_DIR/logs/log-producer.pid"

echo ""
echo "All services started!"
echo ""
echo "Service PIDs:"
echo "  Routing Service: $ROUTING_PID"
echo "  Security Consumer: $SECURITY_PID"
echo "  Performance Consumer: $PERFORMANCE_PID"
echo "  Application Consumer: $APPLICATION_PID"
echo "  System Consumer: $SYSTEM_PID"
echo "  Log Producer: $PRODUCER_PID"
echo ""
echo "Waiting for services to be ready..."
sleep 10

# Check service health
echo ""
echo "Checking service health..."
for port in "${PORTS[@]}"; do
    if check_port "$port"; then
        echo "✓ Port $port is listening"
    else
        echo "✗ Port $port is not listening"
    fi
done

echo ""
echo "Services are starting. Logs are in: $SCRIPT_DIR/logs/"
echo ""
echo "Access points:"
echo "  - Log Producer: http://localhost:8080/actuator/health"
echo "  - Routing Service: http://localhost:8081/actuator/health"
echo "  - Security Consumer: http://localhost:9082/actuator/health"
echo "  - Performance Consumer: http://localhost:9083/actuator/health"
echo "  - Application Consumer: http://localhost:9084/actuator/health"
echo "  - System Consumer: http://localhost:9085/actuator/health"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"

