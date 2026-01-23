#!/bin/bash

set -e

echo "🚀 Setting up Sessionization System"
echo "==================================="

echo "1. Starting Docker Compose services..."
docker-compose up -d

echo ""
echo "2. Waiting for services to initialize (45 seconds)..."
sleep 45

echo ""
echo "3. Checking service health..."
services=("event-producer:8081" "session-processor:8082" "session-analytics:8083" "api-gateway:8080")

for service in "${services[@]}"; do
    IFS=':' read -r name port <<< "$service"
    if curl -f "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
        echo " ✅ $name is healthy"
    else
        echo " ⚠️  $name is not responding yet (may need more time)"
    fi
done

echo ""
echo "4. Verifying Kafka topics..."
docker exec $(docker ps -qf "name=kafka") \
    kafka-topics --bootstrap-server localhost:9092 --list | grep -E "user-events|completed-sessions" && \
    echo " ✅ Kafka topics created" || \
    echo " ⚠️  Kafka topics not yet created (will auto-create on first message)"

echo ""
echo "5. System ready! Access points:"
echo "   - API Gateway: http://localhost:8080"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "6. Try these commands:"
echo "   curl http://localhost:8080/api/analytics/stats | jq"
echo "   curl http://localhost:8080/api/sessions/active/user-1 | jq"
echo ""
echo "Run './load-test.sh' to test the system under load"
echo "Run 'integration-tests/test-sessionization.sh' for full integration tests"
