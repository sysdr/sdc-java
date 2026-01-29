#!/bin/bash

# Cleanup script for Real-Time Log Indexing System
# This script stops all services and removes unused Docker resources

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo "Real-Time Log Indexing System Cleanup"
echo "=========================================="
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Stop Java services
echo "1. Stopping Java services..."
if pgrep -f 'log-producer|log-indexer|search-api' > /dev/null; then
    echo "   Found running Java services, stopping..."
    pkill -f 'log-producer|log-indexer|search-api' || true
    sleep 2
    # Force kill if still running
    pkill -9 -f 'log-producer|log-indexer|search-api' || true
    sleep 1
    echo "   ✓ Java services stopped"
else
    echo "   ✓ No Java services running"
fi
echo ""

# Stop Docker containers
echo "2. Stopping Docker containers..."
if command_exists docker && docker ps -q > /dev/null 2>&1; then
    if [ -f "docker-compose.yml" ]; then
        echo "   Stopping docker-compose services..."
        docker-compose down 2>/dev/null || docker compose down 2>/dev/null || true
        echo "   ✓ Docker containers stopped"
    else
        echo "   ⚠ docker-compose.yml not found"
    fi
else
    echo "   ⚠ Docker not available or no containers running"
fi
echo ""

# Remove unused Docker resources
echo "3. Cleaning up Docker resources..."
if command_exists docker; then
    # Remove stopped containers
    echo "   Removing stopped containers..."
    docker container prune -f > /dev/null 2>&1 || true
    
    # Remove unused networks
    echo "   Removing unused networks..."
    docker network prune -f > /dev/null 2>&1 || true
    
    # Remove unused volumes (be careful with this)
    echo "   Removing unused volumes..."
    docker volume prune -f > /dev/null 2>&1 || true
    
    # Remove unused images (optional - commented out for safety)
    # echo "   Removing unused images..."
    # docker image prune -f > /dev/null 2>&1 || true
    
    echo "   ✓ Docker cleanup completed"
else
    echo "   ⚠ Docker not available"
fi
echo ""

# Remove target directories
echo "4. Removing Maven target directories..."
if [ -d "log-producer/target" ] || [ -d "log-indexer/target" ] || [ -d "search-api/target" ]; then
    find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
    echo "   ✓ Target directories removed"
else
    echo "   ✓ No target directories found"
fi
echo ""

# Remove log files
echo "5. Removing log files..."
find . -maxdepth 1 -type f -name "*.log" -delete 2>/dev/null || true
rm -f /tmp/log-producer.log /tmp/log-indexer.log /tmp/search-api.log 2>/dev/null || true
echo "   ✓ Log files removed"
echo ""

# Remove temporary files
echo "6. Removing temporary files..."
find . -type f -name "*.tmp" -delete 2>/dev/null || true
find . -type f -name "*.temp" -delete 2>/dev/null || true
find . -type f -name "*.pid" -delete 2>/dev/null || true
find . -type f -name "*.lock" -delete 2>/dev/null || true
echo "   ✓ Temporary files removed"
echo ""

# Summary
echo "=========================================="
echo "Cleanup Summary"
echo "=========================================="
echo "✓ Java services stopped"
echo "✓ Docker containers stopped"
echo "✓ Docker resources cleaned"
echo "✓ Target directories removed"
echo "✓ Log files removed"
echo "✓ Temporary files removed"
echo ""
echo "Cleanup completed successfully!"
echo ""
echo "To start services again:"
echo "  1. ./setup.sh              # Start infrastructure"
echo "  2. mvn clean install       # Build services"
echo "  3. ./start-*.sh            # Start services"
echo ""
