#!/bin/bash

echo "🚀 Starting Cluster Membership System..."

# Start infrastructure
echo "Starting infrastructure services..."
docker-compose up -d zookeeper kafka redis postgres

echo "Waiting for infrastructure to be ready..."
sleep 20

# Start cluster services
echo "Starting cluster services..."
docker-compose up -d cluster-coordinator log-producer log-consumer api-gateway

echo "Waiting for cluster to form..."
sleep 15

# Start monitoring
echo "Starting monitoring..."
docker-compose up -d prometheus grafana

# Start dashboard
echo "Starting dashboard..."
docker-compose up -d dashboard

echo ""
echo "✅ System is ready!"
echo ""
echo "📊 Dashboard: http://localhost:3001"
echo "🔍 Cluster Status: http://localhost:8081/cluster/status"
echo "📊 Prometheus: http://localhost:9090"
echo "📈 Grafana: http://localhost:3000 (admin/admin)"
echo "🌐 API Gateway: http://localhost:8080"
echo ""
echo "Run integration tests:"
echo "  cd integration-tests && ./test-cluster-health.sh"
echo ""
echo "Simulate node failure:"
echo "  ./simulate-failure.sh"
