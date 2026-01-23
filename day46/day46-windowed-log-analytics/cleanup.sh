#!/bin/bash

set -e

echo "=========================================="
echo "Cleaning Up Docker Resources"
echo "=========================================="

# Stop and remove containers, networks, and volumes
echo "Stopping and removing containers..."
docker-compose down -v 2>/dev/null || echo "No containers to stop"

# Remove unused Docker resources
echo ""
echo "Removing unused Docker resources..."

# Remove unused containers
echo "  - Removing stopped containers..."
docker container prune -f 2>/dev/null || true

# Remove unused networks
echo "  - Removing unused networks..."
docker network prune -f 2>/dev/null || true

# Remove unused volumes (be careful with this)
echo "  - Removing unused volumes..."
docker volume prune -f 2>/dev/null || true

# Remove unused images (optional - commented out for safety)
# echo "  - Removing unused images..."
# docker image prune -f 2>/dev/null || true

# Remove build cache (optional - can free up significant space)
read -p "Remove Docker build cache? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "  - Removing build cache..."
    docker builder prune -af 2>/dev/null || true
fi

# Clean up target directories
echo ""
echo "Cleaning up build artifacts..."
find . -type d -name 'target' -exec rm -rf {} + 2>/dev/null || true
find . -name '*.class' -delete 2>/dev/null || true
find . -name '*.jar' -not -path '*/target/*' -delete 2>/dev/null || true
echo "  - Removed target directories and build artifacts"

# Show Docker disk usage
echo ""
echo "=========================================="
echo "Docker Disk Usage:"
echo "=========================================="
docker system df

echo ""
echo "=========================================="
echo "Cleanup completed!"
echo "=========================================="
