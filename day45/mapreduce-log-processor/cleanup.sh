#!/bin/bash

echo "=== MapReduce Cleanup Script ==="
echo ""

# Stop Java services
echo "1. Stopping Java services..."
pkill -f 'java -jar' 2>/dev/null
sleep 2
echo "   ✓ Java services stopped"
echo ""

# Stop Docker containers
echo "2. Stopping Docker containers..."
cd "$(dirname "$0")"
docker-compose down 2>/dev/null
echo "   ✓ Docker containers stopped"
echo ""

# Remove unused Docker resources
echo "3. Cleaning up Docker resources..."
docker system prune -f --volumes 2>/dev/null
echo "   ✓ Unused Docker resources removed"
echo ""

# Remove target directories
echo "4. Removing Maven target directories..."
find . -type d -name 'target' -exec rm -rf {} + 2>/dev/null
echo "   ✓ Target directories removed"
echo ""

# Remove log files
echo "5. Cleaning up log files..."
rm -f /tmp/*.log /tmp/*.pid 2>/dev/null
echo "   ✓ Log files cleaned"
echo ""

echo "=== Cleanup Complete ==="
echo ""
echo "All services stopped and resources cleaned up."
