#!/bin/bash

# Cleanup script for Log Encryption System
# Stops all containers and removes unused Docker resources

set -e

echo "🧹 Starting cleanup process..."

# Stop and remove containers, networks, and volumes
echo "📦 Stopping and removing Docker Compose services..."
cd "$(dirname "$0")"
docker-compose down -v --remove-orphans 2>/dev/null || echo "No docker-compose services to stop"

# Stop all running containers
echo "🛑 Stopping all running containers..."
docker stop $(docker ps -aq) 2>/dev/null || echo "No containers to stop"

# Remove all stopped containers
echo "🗑️  Removing stopped containers..."
docker rm $(docker ps -aq) 2>/dev/null || echo "No containers to remove"

# Remove unused images
echo "🖼️  Removing unused Docker images..."
docker image prune -af --filter "dangling=true" 2>/dev/null || echo "No unused images"

# Remove unused volumes
echo "💾 Removing unused Docker volumes..."
docker volume prune -af 2>/dev/null || echo "No unused volumes"

# Remove unused networks
echo "🌐 Removing unused Docker networks..."
docker network prune -af 2>/dev/null || echo "No unused networks"

# Clean build cache (optional - uncomment if needed)
# echo "🧼 Cleaning build cache..."
# docker builder prune -af 2>/dev/null || echo "No build cache to clean"

# Remove target directories
echo "📁 Removing target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || echo "No target directories found"

# Remove compiled class files
echo "🗑️  Removing compiled class files..."
find . -name "*.class" -delete 2>/dev/null || echo "No class files found"

# System cleanup (optional - be careful with this)
echo "🧹 Running Docker system prune..."
docker system prune -af --volumes 2>/dev/null || echo "System prune completed"

echo ""
echo "✅ Cleanup completed!"
echo ""
echo "📊 Remaining Docker resources:"
docker ps -a
echo ""
docker images
echo ""
docker volume ls
echo ""
