#!/bin/bash

# Cleanup script for Log Search System
# Stops all containers and removes unused Docker resources

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "Log Search System Cleanup"
echo "========================================="

# Stop Spring Boot services if running
echo ""
echo "1. Stopping Spring Boot services..."
if [ -f stop-services.sh ]; then
    bash stop-services.sh 2>/dev/null || echo "No services to stop"
else
    echo "   stop-services.sh not found, checking for running processes..."
    pkill -f "log-producer.*spring-boot:run" 2>/dev/null && echo "   Stopped log-producer" || echo "   log-producer not running"
    pkill -f "log-indexer.*spring-boot:run" 2>/dev/null && echo "   Stopped log-indexer" || echo "   log-indexer not running"
    pkill -f "search-api.*spring-boot:run" 2>/dev/null && echo "   Stopped search-api" || echo "   search-api not running"
fi

# Stop Docker containers
echo ""
echo "2. Stopping Docker containers..."
if [ -f docker-compose.yml ]; then
    docker-compose down 2>/dev/null || echo "   No containers to stop"
else
    echo "   docker-compose.yml not found"
fi

# Remove unused Docker resources
echo ""
echo "3. Removing unused Docker resources..."

# Remove stopped containers
echo "   Removing stopped containers..."
docker container prune -f 2>/dev/null || echo "   No stopped containers to remove"

# Remove unused networks
echo "   Removing unused networks..."
docker network prune -f 2>/dev/null || echo "   No unused networks to remove"

# Remove unused volumes (be careful with this)
echo "   Checking for unused volumes..."
UNUSED_VOLUMES=$(docker volume ls -q -f dangling=true 2>/dev/null | wc -l)
if [ "$UNUSED_VOLUMES" -gt 0 ]; then
    echo "   Found $UNUSED_VOLUMES unused volume(s). Skipping removal (use 'docker volume prune -f' manually if needed)"
else
    echo "   No unused volumes found"
fi

# Remove unused images (optional - commented out by default)
# Uncomment the following lines if you want to remove unused images
# echo "   Removing unused images..."
# docker image prune -f 2>/dev/null || echo "   No unused images to remove"

# Remove build artifacts
echo ""
echo "4. Removing build artifacts..."
if [ -d "log-producer/target" ]; then
    rm -rf log-producer/target && echo "   Removed log-producer/target"
fi
if [ -d "log-indexer/target" ]; then
    rm -rf log-indexer/target && echo "   Removed log-indexer/target"
fi
if [ -d "search-api/target" ]; then
    rm -rf search-api/target && echo "   Removed search-api/target"
fi

# Remove log files
echo ""
echo "5. Removing log files..."
rm -f *.log *.pid 2>/dev/null && echo "   Removed log and PID files" || echo "   No log files to remove"

# Summary
echo ""
echo "========================================="
echo "Cleanup Complete!"
echo "========================================="
echo ""
echo "To remove all unused Docker resources (including images), run:"
echo "  docker system prune -a"
echo ""
echo "To start services again, run:"
echo "  docker-compose up -d"
echo "  ./start-services.sh"
echo "========================================="
