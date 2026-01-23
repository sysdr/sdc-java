#!/bin/bash

set -e

echo "🧹 Cleaning up Sliding Window Analytics System"
echo "=============================================="

# Get the project directory
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT"

# Stop all Java services
echo ""
echo "1. Stopping Java services..."
pkill -f "spring-boot:run" 2>/dev/null && echo "   ✅ Java services stopped" || echo "   ℹ️  No Java services running"

# Wait a moment for processes to terminate
sleep 2

# Stop and remove Docker containers
echo ""
echo "2. Stopping Docker containers..."
if [ -f "docker-compose.yml" ]; then
    docker-compose down -v 2>/dev/null && echo "   ✅ Docker containers stopped and removed" || echo "   ℹ️  No Docker containers running"
else
    echo "   ⚠️  docker-compose.yml not found"
fi

# Remove unused Docker resources
echo ""
echo "3. Cleaning up unused Docker resources..."
docker system prune -f --volumes 2>/dev/null && echo "   ✅ Unused Docker resources removed" || echo "   ⚠️  Docker cleanup failed"

# Remove Kafka Streams state directories
echo ""
echo "4. Removing Kafka Streams state directories..."
rm -rf /tmp/kafka-streams* 2>/dev/null && echo "   ✅ Kafka Streams state directories removed" || echo "   ℹ️  No Kafka Streams state directories found"

# Remove log files
echo ""
echo "5. Removing log files..."
find . -name "*.log" -type f -delete 2>/dev/null && echo "   ✅ Log files removed" || echo "   ℹ️  No log files found"

# Remove target directories
echo ""
echo "6. Removing Maven target directories..."
find . -type d -name "target" -exec rm -rf {} + 2>/dev/null && echo "   ✅ Target directories removed" || echo "   ℹ️  No target directories found"

# Summary
echo ""
echo "✅ Cleanup completed!"
echo ""
echo "Remaining resources:"
echo "  - Source code: preserved"
echo "  - Configuration files: preserved"
echo "  - README.md: preserved"
echo ""
echo "To start services again, run: ./start-services.sh"
