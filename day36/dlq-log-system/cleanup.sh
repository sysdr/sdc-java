#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Docker Cleanup Script ==="
echo ""

# Stop all Docker Compose services
echo "1. Stopping Docker Compose services..."
if [ -f "$SCRIPT_DIR/docker-compose.yml" ]; then
    docker-compose down -v 2>/dev/null || true
    echo "   Docker Compose services stopped"
else
    echo "   No docker-compose.yml found"
fi

# Stop any running containers from this project
echo ""
echo "2. Stopping project containers..."
docker ps -a --filter "name=dlq-log-system" --format "{{.Names}}" | while read container; do
    if [ ! -z "$container" ]; then
        echo "   Stopping and removing: $container"
        docker stop "$container" 2>/dev/null || true
        docker rm "$container" 2>/dev/null || true
    fi
done

# Remove unused containers
echo ""
echo "3. Removing unused containers..."
UNUSED_CONTAINERS=$(docker ps -a -q -f "status=exited" 2>/dev/null | wc -l)
if [ "$UNUSED_CONTAINERS" -gt 0 ]; then
    docker container prune -f
    echo "   Removed unused containers"
else
    echo "   No unused containers found"
fi

# Remove unused volumes
echo ""
echo "4. Removing unused volumes..."
UNUSED_VOLUMES=$(docker volume ls -q -f "dangling=true" 2>/dev/null | wc -l)
if [ "$UNUSED_VOLUMES" -gt 0 ]; then
    docker volume prune -f
    echo "   Removed unused volumes"
else
    echo "   No unused volumes found"
fi

# Remove unused images (only those not tagged and not used)
echo ""
echo "5. Removing unused images..."
UNUSED_IMAGES=$(docker images -q -f "dangling=true" 2>/dev/null | wc -l)
if [ "$UNUSED_IMAGES" -gt 0 ]; then
    docker image prune -f
    echo "   Removed unused images"
else
    echo "   No unused images found"
fi

# Remove unused networks
echo ""
echo "6. Removing unused networks..."
UNUSED_NETWORKS=$(docker network ls -q -f "dangling=true" 2>/dev/null | wc -l)
if [ "$UNUSED_NETWORKS" -gt 0 ]; then
    docker network prune -f
    echo "   Removed unused networks"
else
    echo "   No unused networks found"
fi

# Optional: Remove all stopped containers (more aggressive)
# Uncomment below if you want to remove ALL stopped containers
# docker container prune -f
# echo "   All stopped containers removed"

# Optional: Remove all unused images (more aggressive)
# Uncomment below if you want to remove ALL unused images
# docker image prune -a -f
# echo "   All unused images removed"

# Show summary
echo ""
echo "=== Cleanup Summary ==="
echo "Containers: $(docker ps -a -q | wc -l) total"
echo "Images: $(docker images -q | wc -l) total"
echo "Volumes: $(docker volume ls -q | wc -l) total"
echo "Networks: $(docker network ls -q | wc -l) total"
echo ""
echo "Cleanup completed!"

