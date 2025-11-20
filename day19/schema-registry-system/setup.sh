#!/bin/bash
set -e

echo "Starting Schema Registry System infrastructure..."

# Start Docker containers
docker compose up -d

# Wait for PostgreSQL
echo "Waiting for PostgreSQL..."
until docker exec schema-postgres pg_isready -U postgres > /dev/null 2>&1; do
    sleep 1
done
echo "PostgreSQL is ready!"

# Wait for Redis
echo "Waiting for Redis..."
until docker exec schema-redis redis-cli ping > /dev/null 2>&1; do
    sleep 1
done
echo "Redis is ready!"

echo ""
echo "Infrastructure is running!"
echo "  PostgreSQL: localhost:5432"
echo "  Redis:      localhost:6379"
echo "  Prometheus: http://localhost:9090"
echo "  Grafana:    http://localhost:3000 (admin/admin)"
echo ""
echo "To start the services:"
echo "  cd schema-registry && mvn spring-boot:run"
echo "  cd validation-gateway && mvn spring-boot:run"
