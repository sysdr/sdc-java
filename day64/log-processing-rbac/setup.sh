#!/bin/bash

echo "🚀 Setting up RBAC System..."

# Start infrastructure
echo "Starting Docker containers..."
docker-compose up -d

echo "Waiting for services to be ready..."
sleep 30

# Check service health
for service in auth-service api-gateway log-query-service audit-service; do
  until docker-compose ps | grep $service | grep -q "Up"; do
    echo "Waiting for $service..."
    sleep 5
  done
  echo "✅ $service is running"
done

echo ""
echo "🎉 RBAC System is ready!"
echo ""
echo "Services:"
echo "  - Auth Service: http://localhost:8081"
echo "  - API Gateway: http://localhost:8080"
echo "  - Prometheus: http://localhost:9090"
echo "  - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "Test Users:"
echo "  - admin/admin123 (ADMIN, SRE roles)"
echo "  - developer/dev123 (DEVELOPER role, teams: payments, fraud)"
echo "  - analyst/analyst123 (ANALYST role, team: analytics)"
echo ""
echo "Run integration tests: ./integration-tests/test_rbac.sh"
echo "Run load test: ./load-test.sh"
