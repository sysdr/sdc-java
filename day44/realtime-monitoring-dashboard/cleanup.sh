#!/bin/bash

set -e

echo "================================================"
echo "Real-Time Monitoring Dashboard - Cleanup Script"
echo "================================================"
echo ""

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "1. Stopping all Docker Compose services..."
docker-compose down -v 2>&1 | grep -E "(Stopping|Stopped|Removing|Removed)" || echo "   No services running"

echo ""
echo "2. Removing unused Docker resources..."
echo "   - Stopped containers..."
docker container prune -f 2>&1 | grep -E "(Total|Deleted)" || echo "   No stopped containers"

echo "   - Unused networks..."
docker network prune -f 2>&1 | grep -E "(Total|Deleted)" || echo "   No unused networks"

echo "   - Unused images..."
docker image prune -f 2>&1 | grep -E "(Total|Deleted)" || echo "   No unused images"

echo "   - Unused volumes..."
docker volume prune -f 2>&1 | grep -E "(Total|Deleted)" || echo "   No unused volumes"

echo ""
echo "3. Removing target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
find . -type d -name ".mvn" -path "*/target/.mvn" -exec rm -rf {} + 2>/dev/null || true
echo "   Target directories removed"

echo ""
echo "4. Removing build artifacts..."
find . -type f -name "*.jar" ! -path "*/target/*" -delete 2>/dev/null || true
find . -type f -name "*.class" -delete 2>/dev/null || true
find . -type f -name "*.log" -delete 2>/dev/null || true
echo "   Build artifacts removed"

echo ""
echo "5. Cleaning up temporary files..."
find . -type f -name "*.tmp" -delete 2>/dev/null || true
find . -type f -name "*.bak" -delete 2>/dev/null || true
find . -type f -name "*.swp" -delete 2>/dev/null || true
find . -type f -name "*.swo" -delete 2>/dev/null || true
find . -type f -name "*~" -delete 2>/dev/null || true
echo "   Temporary files removed"

echo ""
echo "6. Checking for Docker system cleanup..."
echo "   Run 'docker system prune -a --volumes' manually if you want to remove all unused Docker resources"
echo "   (This will remove all unused images, containers, networks, and volumes)"

echo ""
echo "================================================"
echo "✅ Cleanup completed successfully!"
echo "================================================"
echo ""
echo "Remaining Docker resources:"
docker ps -a --format "table {{.Names}}\t{{.Status}}" 2>/dev/null | head -5 || echo "   No containers"
echo ""
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" 2>/dev/null | head -5 || echo "   No images"
echo ""
echo "To remove all unused Docker resources (including images), run:"
echo "  docker system prune -a --volumes"
echo ""
