#!/bin/bash

set -e

echo "=== Chaos Testing Framework Cleanup Script ==="
echo ""

# Stop Spring Boot services
echo "Stopping Spring Boot services..."
pkill -f "spring-boot:run" 2>/dev/null || echo "No Spring Boot services running"
sleep 2

# Stop Docker containers
echo "Stopping Docker containers..."
cd "$(dirname "$0")"
docker compose down 2>/dev/null || echo "No Docker containers running"

# Remove Docker volumes (optional - uncomment if you want to remove data)
# echo "Removing Docker volumes..."
# docker volume rm chaos-testing-framework_postgres-data 2>/dev/null || true
# docker volume rm chaos-testing-framework_prometheus-data 2>/dev/null || true
# docker volume rm chaos-testing-framework_grafana-data 2>/dev/null || true

# Remove unused Docker resources
echo "Cleaning up unused Docker resources..."
docker system prune -f

# Remove dangling images
echo "Removing dangling Docker images..."
docker image prune -f

# Remove target directories
echo "Removing Maven target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true

# Remove compiled class files
echo "Removing compiled class files..."
find . -type f -name "*.class" -delete 2>/dev/null || true

# Remove log files
echo "Removing log files..."
find . -type f -name "*.log" -delete 2>/dev/null || true
find . -type d -name "logs" -exec rm -rf {} + 2>/dev/null || true

echo ""
echo "=== Cleanup Complete ==="
echo ""
echo "Remaining Docker resources:"
docker ps -a
echo ""
echo "Docker volumes:"
docker volume ls | grep chaos-testing-framework || echo "No chaos-testing-framework volumes found"
echo ""
echo "To remove all Docker volumes (including data), run:"
echo "  docker volume rm chaos-testing-framework_postgres-data chaos-testing-framework_prometheus-data chaos-testing-framework_grafana-data"
