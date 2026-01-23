#!/bin/bash

set -e

echo "🧹 Cleaning up Docker resources for Sessionization System"
echo "========================================================="

# Get the project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "1. Stopping all containers..."
docker-compose down -v 2>/dev/null || echo "   No containers to stop"

echo ""
echo "2. Removing stopped containers..."
docker container prune -f

echo ""
echo "3. Removing unused images..."
docker image prune -f

echo ""
echo "4. Removing unused volumes..."
docker volume prune -f

echo ""
echo "5. Removing unused networks..."
docker network prune -f

echo ""
echo "6. Removing build cache (optional - use with caution)..."
read -p "   Remove build cache? This will free more space but slow down future builds (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker builder prune -f
    echo "   ✅ Build cache removed"
else
    echo "   ⏭️  Build cache kept"
fi

echo ""
echo "7. Checking for dangling images..."
DANGLING=$(docker images -f "dangling=true" -q)
if [ -n "$DANGLING" ]; then
    echo "   Found dangling images, removing..."
    docker rmi $DANGLING 2>/dev/null || echo "   Some images could not be removed (may be in use)"
else
    echo "   ✅ No dangling images found"
fi

echo ""
echo "8. Summary of Docker resources:"
echo "   Containers: $(docker ps -a -q | wc -l)"
echo "   Images: $(docker images -q | wc -l)"
echo "   Volumes: $(docker volume ls -q | wc -l)"
echo "   Networks: $(docker network ls -q | wc -l)"

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "To remove all unused resources (more aggressive):"
echo "  docker system prune -a --volumes"
echo ""
echo "⚠️  Warning: The above command will remove ALL unused images, not just dangling ones!"
