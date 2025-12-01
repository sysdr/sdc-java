#!/bin/bash

echo "🚀 Setting up Day 24: Consistent Hashing System"

# Build all services
echo "Building services..."
mvn clean install -DskipTests

# Start infrastructure
echo "Starting Docker Compose..."
docker-compose up -d

echo "⏳ Waiting for services to be ready..."
sleep 30

# Check service health
echo "Checking service health..."
curl -f http://localhost:8080/api/logs/health || echo "Gateway not ready"
curl -f http://localhost:8081/actuator/health || echo "Coordinator not ready"
curl -f http://localhost:8082/api/storage/health || echo "Storage node 1 not ready"

echo ""
echo "✅ System is ready!"
echo ""
echo "Services:"
echo "  - API Gateway: http://localhost:8080"
echo "  - Storage Coordinator: http://localhost:8081"
echo "  - Storage Nodes: http://localhost:8082-8084"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo "  - Dashboard: http://localhost:8085"
echo ""
echo "Next steps:"
echo "  1. Open Dashboard: http://localhost:8085"
echo "  2. Run load tests: ./load-test.sh"
echo "  3. Check distribution: curl http://localhost:8081/api/coordinator/metrics/distribution"
echo "  4. View metrics in Grafana"
