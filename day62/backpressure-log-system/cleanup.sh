#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Backpressure Log System Cleanup ==="
echo ""

# Stop Java services
echo "1. Stopping Java services..."
pkill -f 'spring-boot:run' 2>/dev/null || echo "   No Spring Boot services running"
pkill -f 'ApiGateway\|LogConsumer' 2>/dev/null || echo "   No application services running"
sleep 2
echo "   ✓ Java services stopped"
echo ""

# Stop and remove Docker containers
echo "2. Stopping Docker containers..."
if [ -f "docker-compose.yml" ]; then
    docker-compose down 2>/dev/null || echo "   No containers to stop"
    echo "   ✓ Docker containers stopped and removed"
else
    echo "   ⚠ docker-compose.yml not found"
fi
echo ""

# Remove Docker volumes
echo "3. Removing Docker volumes..."
docker volume ls -q | grep backpressure-log-system | xargs -r docker volume rm 2>/dev/null || echo "   No volumes to remove"
echo "   ✓ Docker volumes removed"
echo ""

# Remove Docker networks
echo "4. Removing Docker networks..."
docker network ls -q | grep backpressure-log-system | xargs -r docker network rm 2>/dev/null || echo "   No networks to remove"
echo "   ✓ Docker networks removed"
echo ""

# Prune unused Docker resources
echo "5. Pruning unused Docker resources..."
docker system prune -f --volumes 2>/dev/null || echo "   Docker prune completed"
echo "   ✓ Unused Docker resources removed"
echo ""

# Remove target directories
echo "6. Removing Maven target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || echo "   No target directories found"
echo "   ✓ Target directories removed"
echo ""

# Remove log files
echo "7. Cleaning up log files..."
rm -rf logs/ 2>/dev/null || echo "   No logs directory found"
rm -f *.log 2>/dev/null || echo "   No log files found"
rm -f /tmp/api-gateway*.log /tmp/log-consumer*.log 2>/dev/null || echo "   No temporary log files found"
echo "   ✓ Log files removed"
echo ""

# Remove PID files
echo "8. Removing PID files..."
find . -name "*.pid" -delete 2>/dev/null || echo "   No PID files found"
echo "   ✓ PID files removed"
echo ""

echo "=== Cleanup Complete ==="
echo ""
echo "Summary:"
echo "  - Java services: Stopped"
echo "  - Docker containers: Stopped and removed"
echo "  - Docker volumes: Removed"
echo "  - Docker networks: Removed"
echo "  - Unused Docker resources: Pruned"
echo "  - Maven target directories: Removed"
echo "  - Log files: Removed"
echo ""
echo "To start services again:"
echo "  ./start-all-services.sh"
echo ""
