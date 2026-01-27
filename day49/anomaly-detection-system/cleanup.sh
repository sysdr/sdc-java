#!/bin/bash

# Cleanup script for Anomaly Detection System
# This script stops all containers and removes unused Docker resources

set -e

echo "=== Anomaly Detection System Cleanup ==="
echo ""

# Get the directory where the script is located
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# Stop and remove containers
echo "1. Stopping and removing Docker Compose containers..."
if [ -f docker-compose.yml ]; then
    docker-compose down -v 2>/dev/null || true
    echo "   ✓ Containers stopped and removed"
else
    echo "   ⚠ docker-compose.yml not found, skipping..."
fi

# Remove unused Docker resources
echo ""
echo "2. Cleaning up unused Docker resources..."

# Remove unused containers
echo "   - Removing stopped containers..."
docker container prune -f 2>/dev/null || true

# Remove unused images
echo "   - Removing unused images..."
docker image prune -f 2>/dev/null || true

# Remove unused volumes
echo "   - Removing unused volumes..."
docker volume prune -f 2>/dev/null || true

# Remove unused networks
echo "   - Removing unused networks..."
docker network prune -f 2>/dev/null || true

# Optional: Remove all unused resources at once (more aggressive)
echo ""
read -p "   Remove ALL unused Docker resources (images, containers, networks, volumes)? [y/N]: " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "   - Performing full cleanup..."
    docker system prune -a --volumes -f 2>/dev/null || true
    echo "   ✓ Full cleanup completed"
else
    echo "   ⚠ Skipping full cleanup"
fi

# Remove target directories
echo ""
echo "3. Removing Maven target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
echo "   ✓ Target directories removed"

# Summary
echo ""
echo "=== Cleanup Summary ==="
echo "✓ Docker containers stopped and removed"
echo "✓ Unused Docker resources cleaned"
echo "✓ Maven target directories removed"
echo ""
echo "Cleanup completed successfully!"
echo ""
echo "To start the system again, run:"
echo "  docker-compose up -d --build"
