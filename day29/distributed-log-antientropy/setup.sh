#!/bin/bash

echo "====================================="
echo "Day 29: Anti-Entropy System Setup"
echo "====================================="

# Build and start all services
echo "Building Docker images..."
docker compose build

echo "Starting services..."
docker compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Health check
echo "Checking service health..."
services=("node1:8081" "node2:8082" "node3:8083" "merkle-tree-service:8084" "anti-entropy-coordinator:8085" "read-repair-service:8086" "hint-manager:8087" "api-gateway:8080")

for service in "${services[@]}"; do
    url="http://$service/api/health"
    if curl -s "$url" | grep -q "UP"; then
        echo "✓ $service is healthy"
    else
        echo "✗ $service is not responding"
    fi
done

echo ""
echo "====================================="
echo "System is ready!"
echo "====================================="
echo ""
echo "API Gateway: http://localhost:8080"
echo "Storage Nodes: http://localhost:8081-8083"
echo "Merkle Tree Service: http://localhost:8084"
echo "Anti-Entropy Coordinator: http://localhost:8085"
echo "Read Repair Service: http://localhost:8086"
echo "Hint Manager: http://localhost:8087"
echo "Prometheus: http://localhost:9090"
echo "Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "To test the system, run:"
echo "  bash load-test.sh"
echo ""
echo "To view logs:"
echo "  docker-compose logs -f [service-name]"
echo ""
echo "To stop:"
echo "  docker compose down"
echo "====================================="
