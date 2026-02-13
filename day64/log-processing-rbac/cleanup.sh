#!/bin/bash

# Cleanup script for RBAC System
# Stops all containers and removes unused Docker resources

set -e

echo "🧹 Starting cleanup process..."

# Stop and remove containers from docker-compose
if [ -f "docker-compose.yml" ]; then
    echo "Stopping docker-compose services..."
    docker-compose down -v 2>/dev/null || true
    echo "✅ Docker-compose services stopped"
fi

# Stop all running containers
echo "Stopping all running containers..."
RUNNING_CONTAINERS=$(docker ps -q)
if [ -n "$RUNNING_CONTAINERS" ]; then
    docker stop $RUNNING_CONTAINERS 2>/dev/null || true
    echo "✅ All running containers stopped"
else
    echo "ℹ️  No running containers found"
fi

# Remove all stopped containers
echo "Removing stopped containers..."
STOPPED_CONTAINERS=$(docker ps -a -q)
if [ -n "$STOPPED_CONTAINERS" ]; then
    docker rm $STOPPED_CONTAINERS 2>/dev/null || true
    echo "✅ All stopped containers removed"
else
    echo "ℹ️  No stopped containers found"
fi

# Remove unused images
echo "Removing unused Docker images..."
docker image prune -f 2>/dev/null || true
echo "✅ Unused images removed"

# Remove unused volumes
echo "Removing unused Docker volumes..."
docker volume prune -f 2>/dev/null || true
echo "✅ Unused volumes removed"

# Remove unused networks
echo "Removing unused Docker networks..."
docker network prune -f 2>/dev/null || true
echo "✅ Unused networks removed"

# Remove build cache (optional - uncomment if needed)
# echo "Removing Docker build cache..."
# docker builder prune -f 2>/dev/null || true
# echo "✅ Build cache removed"

# Clean up Maven target directories
echo "Cleaning up Maven target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
echo "✅ Target directories removed"

# Summary
echo ""
echo "🎉 Cleanup completed successfully!"
echo ""
echo "Remaining Docker resources:"
echo "  Images: $(docker images -q | wc -l)"
echo "  Containers: $(docker ps -a -q | wc -l)"
echo "  Volumes: $(docker volume ls -q | wc -l)"
echo "  Networks: $(docker network ls -q | wc -l)"
