#!/bin/bash

set -e

echo "==================================="
echo "Docker Cleanup Script"
echo "==================================="
echo ""

# Stop all containers
echo "1. Stopping all containers..."
docker compose down 2>/dev/null || true
docker ps -a --format '{{.Names}}' 2>/dev/null | grep day52 | xargs -r docker stop 2>/dev/null || true
echo "✓ Containers stopped"
echo ""

# Remove containers
echo "2. Removing containers..."
docker ps -a --format '{{.Names}}' 2>/dev/null | grep day52 | xargs -r docker rm 2>/dev/null || true
echo "✓ Containers removed"
echo ""

# Remove images
echo "3. Removing Docker images..."
docker images --format '{{.Repository}}:{{.Tag}}' 2>/dev/null | grep day52-inverted-index-search | xargs -r docker rmi 2>/dev/null || true
echo "✓ Images removed"
echo ""

# Remove volumes
echo "4. Removing volumes..."
docker volume ls --format '{{.Name}}' 2>/dev/null | grep day52 | xargs -r docker volume rm 2>/dev/null || true
echo "✓ Volumes removed"
echo ""

# Prune unused resources
echo "5. Pruning unused Docker resources..."
docker system prune -f --volumes 2>/dev/null || true
echo "✓ Unused resources pruned"
echo ""

# Remove target directories
echo "6. Removing target directories..."
find . -type d -name 'target' -exec rm -rf {} + 2>/dev/null || true
echo "✓ Target directories removed"
echo ""

echo "==================================="
echo "Cleanup completed!"
echo "==================================="
echo ""
echo "Remaining Docker resources:"
docker ps -a --format '  {{.Names}}' 2>/dev/null | grep day52 || echo "  (none)"
echo ""
docker images --format '  {{.Repository}}:{{.Tag}}' 2>/dev/null | grep day52 || echo "  (none)"
echo ""
