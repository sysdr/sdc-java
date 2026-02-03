#!/bin/bash

set -e

echo "🧹 Starting cleanup process..."
echo "================================"

# Stop Java services
echo ""
echo "1. Stopping Java services..."
pkill -f 'api-gateway-1.0.0.jar' 2>/dev/null && echo "   ✓ API Gateway stopped" || echo "   - API Gateway not running"
pkill -f 'log-consumer-1.0.0.jar' 2>/dev/null && echo "   ✓ Log Consumer stopped" || echo "   - Log Consumer not running"
sleep 2

# Stop Docker containers
echo ""
echo "2. Stopping Docker containers..."
cd "$(dirname "$0")"
if [ -f "docker-compose.yml" ]; then
    docker-compose down 2>/dev/null && echo "   ✓ Docker containers stopped" || echo "   - No containers to stop"
else
    echo "   - docker-compose.yml not found"
fi

# Remove stopped containers
echo ""
echo "3. Removing stopped containers..."
docker container prune -f > /dev/null 2>&1 && echo "   ✓ Stopped containers removed" || echo "   - No stopped containers"

# Remove unused images
echo ""
echo "4. Removing unused Docker images..."
docker image prune -a -f > /dev/null 2>&1 && echo "   ✓ Unused images removed" || echo "   - No unused images"

# Remove unused volumes
echo ""
echo "5. Removing unused Docker volumes..."
docker volume prune -f > /dev/null 2>&1 && echo "   ✓ Unused volumes removed" || echo "   - No unused volumes"

# Remove unused networks
echo ""
echo "6. Removing unused Docker networks..."
docker network prune -f > /dev/null 2>&1 && echo "   ✓ Unused networks removed" || echo "   - No unused networks"

# Remove build artifacts
echo ""
echo "7. Removing build artifacts..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null && echo "   ✓ Maven target directories removed" || echo "   - No target directories found"
find . -type d -name ".idea" -exec rm -rf {} + 2>/dev/null && echo "   ✓ .idea directories removed" || echo "   - No .idea directories"
find . -type d -name ".vscode" -exec rm -rf {} + 2>/dev/null && echo "   ✓ .vscode directories removed" || echo "   - No .vscode directories"

# Remove Python artifacts
echo ""
echo "8. Removing Python artifacts..."
find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null && echo "   ✓ __pycache__ directories removed" || echo "   - No __pycache__ directories"
find . -type f -name "*.pyc" -delete 2>/dev/null && echo "   ✓ .pyc files removed" || echo "   - No .pyc files"
find . -type d -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null && echo "   ✓ .pytest_cache directories removed" || echo "   - No .pytest_cache directories"
find . -type d -name "venv" -exec rm -rf {} + 2>/dev/null && echo "   ✓ venv directories removed" || echo "   - No venv directories"
find . -type d -name ".venv" -exec rm -rf {} + 2>/dev/null && echo "   ✓ .venv directories removed" || echo "   - No .venv directories"

# Remove Node.js artifacts
echo ""
echo "9. Removing Node.js artifacts..."
find . -type d -name "node_modules" -exec rm -rf {} + 2>/dev/null && echo "   ✓ node_modules directories removed" || echo "   - No node_modules directories"

# Remove Istio files
echo ""
echo "10. Removing Istio files..."
find . -type d -name "*istio*" -exec rm -rf {} + 2>/dev/null && echo "   ✓ Istio directories removed" || echo "   - No Istio directories"
find . -type f -name "*istio*" -delete 2>/dev/null && echo "   ✓ Istio files removed" || echo "   - No Istio files"

# Remove log files
echo ""
echo "11. Removing log files..."
find . -type f -name "*.log" -delete 2>/dev/null && echo "   ✓ Log files removed" || echo "   - No log files"
rm -f /tmp/api-gateway.log /tmp/log-consumer-*.log 2>/dev/null && echo "   ✓ Service log files removed" || echo "   - No service log files"

# Remove temporary files
echo ""
echo "12. Removing temporary files..."
find . -type f -name "*.tmp" -delete 2>/dev/null && echo "   ✓ Temporary files removed" || echo "   - No temporary files"
find . -type f -name ".DS_Store" -delete 2>/dev/null && echo "   ✓ .DS_Store files removed" || echo "   - No .DS_Store files"

# Summary
echo ""
echo "================================"
echo "✅ Cleanup completed!"
echo ""
echo "Docker resources summary:"
docker system df 2>/dev/null || echo "Docker not available"
echo ""
echo "Remaining Docker containers:"
docker ps -a --format "table {{.Names}}\t{{.Status}}" 2>/dev/null | head -5 || echo "No containers"
echo ""
