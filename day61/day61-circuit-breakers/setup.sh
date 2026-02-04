#!/bin/bash
# =============================================================================
# setup.sh — Bootstrap the Day 61 infrastructure
# =============================================================================
# 1. Starts all Docker services
# 2. Waits for Kafka to be ready
# 3. Creates the "log-events" topic with 3 partitions
# =============================================================================
set -euo pipefail

echo "=============================================="
echo " Day 61 — Infrastructure Bootstrap"
echo "=============================================="

# 1. Pull images and start all services
echo "[1/3] Starting Docker services..."
docker compose up -d

# 2. Wait for Kafka to be healthy
echo "[2/3] Waiting for Kafka to be ready..."
KAFKA_READY=false
for i in $(seq 1 30); do
    if docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list >/dev/null 2>&1; then
        KAFKA_READY=true
        echo "       Kafka is ready."
        break
    fi
    echo "       Waiting... (attempt $i/30)"
    sleep 5
done

if [ "$KAFKA_READY" = false ]; then
    echo "ERROR: Kafka did not become ready in time."
    exit 1
fi

# 3. Create the log-events topic
echo "[3/3] Creating 'log-events' topic..."
docker exec kafka kafka-topics \
    --bootstrap-server localhost:9092 \
    --create \
    --topic log-events \
    --partitions 3 \
    --replication-factor 1 \
    --if-not-exists

echo ""
echo "=============================================="
echo " Infrastructure ready!"
echo "=============================================="
echo ""
echo "  API Gateway:  http://localhost:8080"
echo "  Prometheus:   http://localhost:9090"
echo "  Grafana:      http://localhost:3000"
echo ""
echo "  Run load tests:  ./load-test.sh"
echo "  Run integration: ./integration-tests/run-tests.sh"
echo "=============================================="
