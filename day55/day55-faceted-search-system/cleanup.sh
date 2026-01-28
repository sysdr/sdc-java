#!/bin/bash

set -e

echo "🧹 Cleaning up Docker resources for Faceted Search System..."

# Stop and remove containers
echo "📦 Stopping and removing containers..."
if [ -f docker-compose.yml ]; then
    docker-compose down --remove-orphans 2>/dev/null || true
fi

# Remove containers by name pattern
echo "🗑️  Removing containers..."
docker ps -a --format '{{.Names}}' | grep -E 'day55|faceted-search' | xargs -r docker rm -f 2>/dev/null || true

# Remove images
echo "🖼️  Removing images..."
docker images --format '{{.Repository}}:{{.Tag}}' | grep -E 'day55|faceted-search' | xargs -r docker rmi -f 2>/dev/null || true

# Remove volumes
echo "💾 Removing volumes..."
docker volume ls --format '{{.Name}}' | grep -E 'day55|faceted-search|elasticsearch|grafana' | xargs -r docker volume rm 2>/dev/null || true

# Remove networks
echo "🌐 Removing networks..."
docker network ls --format '{{.Name}}' | grep -E 'day55|faceted-search' | xargs -r docker network rm 2>/dev/null || true

# Clean up unused Docker resources
echo "🧽 Cleaning up unused Docker resources..."
docker system prune -f --volumes 2>/dev/null || true

# Remove target directories
echo "📁 Removing target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true

# Remove .jar files
echo "🗂️  Removing JAR files..."
find . -type f -name "*.jar" -not -path "*/target/*" -delete 2>/dev/null || true
find . -type f -name "*.jar.original" -delete 2>/dev/null || true

echo ""
echo "✅ Cleanup completed!"
echo ""
echo "Remaining Docker resources:"
docker ps -a --format 'table {{.Names}}\t{{.Status}}' | grep -E 'day55|faceted' || echo "  No matching containers found"
echo ""
docker images --format 'table {{.Repository}}\t{{.Tag}}' | grep -E 'day55|faceted' || echo "  No matching images found"
echo ""
