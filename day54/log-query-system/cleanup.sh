#!/bin/bash

set -e

echo "🧹 Starting cleanup of Docker resources..."

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Stop and remove containers, networks
echo "📦 Stopping and removing containers..."
if [ -f docker-compose.yml ]; then
    docker-compose down -v 2>/dev/null || true
    echo "✅ Docker Compose services stopped and removed"
else
    echo "⚠️  docker-compose.yml not found, skipping compose cleanup"
fi

# Remove unused Docker resources
echo "🗑️  Removing unused Docker resources..."

# Remove stopped containers
echo "  - Removing stopped containers..."
docker container prune -f 2>/dev/null || true

# Remove unused images
echo "  - Removing unused images..."
docker image prune -f 2>/dev/null || true

# Remove unused volumes
echo "  - Removing unused volumes..."
docker volume prune -f 2>/dev/null || true

# Remove unused networks
echo "  - Removing unused networks..."
docker network prune -f 2>/dev/null || true

# Remove build cache (optional - uncomment if you want to remove build cache)
# echo "  - Removing build cache..."
# docker builder prune -f 2>/dev/null || true

# Remove target directories
echo "📁 Removing target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
echo "✅ Target directories removed"

# Remove any .class files
echo "🗑️  Removing compiled .class files..."
find . -type f -name "*.class" -delete 2>/dev/null || true
echo "✅ .class files removed"

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "Remaining Docker resources:"
docker ps -a --format "table {{.Names}}\t{{.Status}}" 2>/dev/null | head -5 || echo "No containers found"
echo ""
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" 2>/dev/null | head -5 || echo "No images found"
