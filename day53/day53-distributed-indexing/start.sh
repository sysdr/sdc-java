#!/bin/bash

set -e

echo "Starting Distributed Indexing System..."
echo ""

# Build and start services
echo "Building and starting Docker containers..."
docker-compose up -d --build

echo ""
echo "Waiting for services to be healthy..."
sleep 45

# Check service health
echo ""
echo "Checking service health..."
services=("index-node-1:8081" "index-node-2:8082" "index-node-3:8083" "query-coordinator:8080" "shard-router:8090" "log-producer:8095")

for service in "${services[@]}"; do
  IFS=':' read -r name port <<< "$service"
  if curl -sf "http://localhost:$port/api/health" > /dev/null; then
    echo "✓ $name is healthy"
  else
    echo "✗ $name is not responding"
  fi
done

echo ""
echo "=== System Started Successfully ==="
echo ""
echo "Service URLs:"
echo "  Query Coordinator: http://localhost:8080"
echo "  Shard Router: http://localhost:8090"
echo "  Index Node 1: http://localhost:8081"
echo "  Index Node 2: http://localhost:8082"
echo "  Index Node 3: http://localhost:8083"
echo "  Log Producer: http://localhost:8095"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Next steps:"
echo "  1. Wait 1 minute for logs to be generated and indexed"
echo "  2. Run integration tests: ./integration-tests/test-distributed-indexing.sh"
echo "  3. Run load test: ./load-test.sh"
echo "  4. Query logs: curl 'http://localhost:8080/api/search?q=error&limit=10'"
echo "  5. Check metrics in Grafana: http://localhost:3000"
echo ""
