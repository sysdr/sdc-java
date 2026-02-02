#!/bin/bash

# Cleanup script for Log Search API project
# Stops all containers and removes unused Docker resources

set -e

echo "🧹 Starting cleanup process..."
echo "================================"

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Stop Java services if running
echo ""
echo "1. Stopping Java services..."
if [ -f logs/api-gateway.pid ]; then
    PID=$(cat logs/api-gateway.pid)
    if ps -p "$PID" > /dev/null 2>&1; then
        kill "$PID" 2>/dev/null || true
        echo "   ✅ Stopped API Gateway (PID: $PID)"
    fi
    rm -f logs/api-gateway.pid
fi

if [ -f logs/log-producer.pid ]; then
    PID=$(cat logs/log-producer.pid)
    if ps -p "$PID" > /dev/null 2>&1; then
        kill "$PID" 2>/dev/null || true
        echo "   ✅ Stopped Log Producer (PID: $PID)"
    fi
    rm -f logs/log-producer.pid
fi

# Kill any remaining Java processes for this project
pkill -f "api-gateway.*jar" 2>/dev/null && echo "   ✅ Stopped remaining API Gateway processes" || true
pkill -f "log-producer.*jar" 2>/dev/null && echo "   ✅ Stopped remaining Log Producer processes" || true

# Stop Docker Compose services
echo ""
echo "2. Stopping Docker Compose services..."
if [ -f docker-compose.yml ]; then
    docker-compose down 2>/dev/null || true
    echo "   ✅ Docker Compose services stopped"
else
    echo "   ⚠️  docker-compose.yml not found, skipping"
fi

# Remove unused Docker resources
echo ""
echo "3. Cleaning up Docker resources..."

# Remove stopped containers
STOPPED_CONTAINERS=$(docker ps -a -q -f "status=exited" 2>/dev/null | wc -l)
if [ "$STOPPED_CONTAINERS" -gt 0 ]; then
    docker container prune -f > /dev/null 2>&1
    echo "   ✅ Removed stopped containers"
fi

# Remove unused networks
UNUSED_NETWORKS=$(docker network ls -q -f "dangling=true" 2>/dev/null | wc -l)
if [ "$UNUSED_NETWORKS" -gt 0 ]; then
    docker network prune -f > /dev/null 2>&1
    echo "   ✅ Removed unused networks"
fi

# Remove unused volumes (be careful with this)
echo "   ⚠️  Skipping volume cleanup (use 'docker volume prune' manually if needed)"

# Remove unused images (only dangling)
DANGLING_IMAGES=$(docker images -q -f "dangling=true" 2>/dev/null | wc -l)
if [ "$DANGLING_IMAGES" -gt 0 ]; then
    docker image prune -f > /dev/null 2>&1
    echo "   ✅ Removed dangling images"
fi

# Clean up log files
echo ""
echo "4. Cleaning up log files..."
if [ -d logs ]; then
    rm -f logs/*.log logs/*.pid 2>/dev/null || true
    echo "   ✅ Log files cleaned"
fi

# Remove target directories
echo ""
echo "5. Removing build artifacts..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
find . -type d -name ".mvn" -exec rm -rf {} + 2>/dev/null || true
echo "   ✅ Build artifacts removed"

echo ""
echo "✅ Cleanup complete!"
echo ""
echo "Remaining Docker resources:"
echo "  Containers: $(docker ps -a | wc -l)"
echo "  Images: $(docker images | wc -l)"
echo "  Networks: $(docker network ls | wc -l)"
echo "  Volumes: $(docker volume ls | wc -l)"
echo ""
echo "To remove all unused Docker resources (including volumes), run:"
echo "  docker system prune -a --volumes"
