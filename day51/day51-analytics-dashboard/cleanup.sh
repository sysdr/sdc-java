#!/bin/bash

set -e

echo "🧹 Cleaning up Docker resources..."

# Stop and remove containers, networks, and volumes
echo "Stopping and removing containers..."
docker-compose down -v 2>/dev/null || true

# Remove unused Docker resources
echo "Removing unused Docker images..."
docker image prune -f

echo "Removing unused Docker volumes..."
docker volume prune -f

echo "Removing unused Docker networks..."
docker network prune -f

# Remove dangling images
echo "Removing dangling images..."
docker image prune -a -f --filter "dangling=true" 2>/dev/null || true

# Optional: Remove all stopped containers
read -p "Do you want to remove all stopped containers? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing all stopped containers..."
    docker container prune -f
fi

# Optional: Remove all unused images (not just dangling)
read -p "Do you want to remove all unused images? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Removing all unused images..."
    docker image prune -a -f
fi

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "Remaining Docker resources:"
docker ps -a --format "table {{.Names}}\t{{.Status}}" | head -5
echo ""
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | head -5
