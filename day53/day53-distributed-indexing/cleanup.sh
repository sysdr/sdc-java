#!/bin/bash

set -e

echo "=== Docker Cleanup Script ==="
echo ""

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "1. Stopping all containers..."
docker-compose down 2>/dev/null || echo "   No containers to stop"
echo ""

echo "2. Removing stopped containers..."
docker container prune -f 2>/dev/null || echo "   No stopped containers"
echo ""

echo "3. Removing unused images..."
docker image prune -f 2>/dev/null || echo "   No unused images"
echo ""

echo "4. Removing unused volumes..."
docker volume prune -f 2>/dev/null || echo "   No unused volumes"
echo ""

echo "5. Removing unused networks..."
docker network prune -f 2>/dev/null || echo "   No unused networks"
echo ""

echo "6. Removing build cache (optional - use with caution)..."
read -p "   Remove build cache? This will free more space but slow down future builds (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker builder prune -f 2>/dev/null || echo "   No build cache to remove"
else
    echo "   Skipping build cache removal"
fi
echo ""

echo "7. Checking for remaining containers..."
REMAINING=$(docker ps -aq 2>/dev/null | wc -l)
if [ "$REMAINING" -gt 0 ]; then
    echo "   Warning: $REMAINING container(s) still exist"
    docker ps -a --format "table {{.Names}}\t{{.Status}}"
    read -p "   Remove all remaining containers? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        docker rm -f $(docker ps -aq) 2>/dev/null || echo "   No containers to remove"
    fi
else
    echo "   ✓ No containers remaining"
fi
echo ""

echo "=== Cleanup Summary ==="
echo "Docker system info:"
docker system df
echo ""

echo "✅ Cleanup complete!"
echo ""
echo "To remove everything (images, volumes, networks, build cache):"
echo "  docker system prune -a --volumes"
echo ""
echo "⚠️  Warning: The above command will remove ALL unused Docker resources!"
