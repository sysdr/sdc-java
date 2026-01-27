#!/bin/bash

echo "🧹 Cleaning up Alert Generation System"
echo "======================================"

# Stop all containers
echo ""
echo "1. Stopping Docker containers..."
cd "$(dirname "$0")"
docker-compose down 2>/dev/null || echo "   No docker-compose containers to stop"

# Stop any remaining containers with alert in name
echo ""
echo "2. Stopping remaining alert-related containers..."
docker ps -a --filter "name=alert" --format "{{.Names}}" | while read container; do
    if [ -n "$container" ]; then
        echo "   Stopping: $container"
        docker stop "$container" 2>/dev/null || true
        docker rm "$container" 2>/dev/null || true
    fi
done

# Remove unused Docker resources
echo ""
echo "3. Removing unused Docker resources..."
echo "   - Pruning stopped containers..."
docker container prune -f 2>/dev/null || true

echo "   - Pruning unused networks..."
docker network prune -f 2>/dev/null || true

echo "   - Pruning unused volumes..."
docker volume prune -f 2>/dev/null || true

echo "   - Pruning unused images..."
docker image prune -f 2>/dev/null || true

# Remove target directories
echo ""
echo "4. Removing Maven target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
echo "   ✅ Target directories removed"

# Remove Docker volumes if they exist
echo ""
echo "5. Removing Docker volumes..."
docker volume ls | grep -E "(alert|postgres|prometheus|grafana)" | awk '{print $2}' | while read volume; do
    if [ -n "$volume" ]; then
        echo "   Removing volume: $volume"
        docker volume rm "$volume" 2>/dev/null || true
    fi
done

# Clean up any remaining Docker resources
echo ""
echo "6. Final cleanup..."
docker system prune -f --volumes 2>/dev/null || true

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "Remaining Docker resources:"
docker ps -a --filter "name=alert" --format "table {{.Names}}\t{{.Status}}" || echo "   No alert-related containers found"

echo ""
echo "To remove all unused Docker resources (images, containers, networks, volumes):"
echo "   docker system prune -a --volumes"
