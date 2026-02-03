#!/bin/bash
# =============================================================================
# setup.sh – Boots the full multi-region stack
#
# Prerequisites:
#   - Docker Desktop (or Docker Engine + Compose plugin) running
#   - Ports 8080-8084, 9090, 9092-9095, 3000, 5432, 6379 available
#
# Usage: ./setup.sh
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=============================================="
echo "  Day 60: Multi-Region Stack Bootstrap       "
echo "=============================================="

# ── Step 1: Build Maven modules ──────────────────────────────────────────────
echo ""
echo "[1/4] Building Maven modules..."
# Install parent POM first
mvn install -N -q 2>/dev/null || true
mvn clean package -DskipTests -U -q 2>/dev/null || {
    echo "  ⚠ Maven build failed. Continuing with Docker-based build..."
    # Fallback: Docker will build the images
}

# ── Step 2: Pull Docker images ───────────────────────────────────────────────
echo ""
echo "[2/4] Pulling Docker images (this may take a minute)..."
docker compose pull --quiet 2>/dev/null || true

# ── Step 3: Start infrastructure first ───────────────────────────────────────
echo ""
echo "[3/4] Starting infrastructure services (ZooKeeper, Kafka, Redis, Postgres)..."
docker compose up -d zookeeper
echo "      Waiting for ZooKeeper to be healthy..."
for i in $(seq 1 30); do
    if docker compose exec zookeeper sh -c "nc -z localhost 2181" 2>/dev/null; then
        echo "      ✓ ZooKeeper healthy"
        break
    fi
    echo "      ... waiting ($i/30)"
    sleep 2
done

docker compose up -d kafka-region-a kafka-region-b
echo "      Waiting for Kafka brokers..."
sleep 15  # Kafka needs time to register with ZooKeeper

docker compose up -d redis postgres
echo "      Waiting for Redis and Postgres..."
sleep 5

# ── Step 4: Start replication and application services ───────────────────────
echo ""
echo "[4/4] Starting MirrorMaker and application services..."
docker compose up -d mirrormaker-a-to-b mirrormaker-b-to-a
sleep 5  # Let MirrorMaker initialize

docker compose up -d log-producer-a log-producer-b
sleep 3

docker compose up -d log-consumer-a log-consumer-b
sleep 3

docker compose up -d api-gateway
sleep 3

docker compose up -d prometheus grafana

echo ""
echo "=============================================="
echo "  ✓ Stack is UP                              "
echo "=============================================="
echo ""
echo "  Service URLs:"
echo "    API Gateway:        http://localhost:8080"
echo "    Producer A:         http://localhost:8081"
echo "    Producer B:         http://localhost:8083"
echo "    Consumer A:         http://localhost:8082"
echo "    Consumer B:         http://localhost:8084"
echo "    Prometheus:         http://localhost:9090"
echo "    Grafana:            http://localhost:3000"
echo "    PostgreSQL:         localhost:5432 (loguser/[password from env])"
echo "    Redis:              localhost:6379"
echo ""
echo "  Quick smoke test:"
echo "    curl -X POST http://localhost:8080/api/logs \\"
echo "      -H 'Content-Type: application/json' \\"
echo "      -H 'X-Region: region-a' \\"
echo "      -d '{\"serviceName\":\"smoke-test\",\"level\":\"INFO\",\"message\":\"Hello from Day 60\",\"correlationId\":\"trace-001\"}'"
echo ""
echo "  Run load tests:  ./load-test.sh"
echo "=============================================="
