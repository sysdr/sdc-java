#!/bin/bash

echo "🚀 Starting Partitioned Log Storage Services..."
echo ""

# Check if JARs exist
if [ ! -f "query-service/target/query-service-1.0.0.jar" ]; then
    echo "❌ JAR files not found. Please run './setup.sh' first to build the services."
    exit 1
fi

# Start Query Service
echo "Starting Query Service..."
if lsof -i :8084 > /dev/null 2>&1; then
    echo "  ⚠️  Query Service is already running on port 8084"
else
    nohup java -jar query-service/target/query-service-1.0.0.jar > query-service.log 2>&1 &
    echo "  ✅ Query Service started (PID: $!)"
    echo "  📝 Logs: query-service.log"
fi

# Wait a bit for service to start
sleep 3

# Check status
echo ""
echo "Service Status:"
if curl -s http://localhost:8084/api/query/health > /dev/null 2>&1; then
    echo "  ✅ Query Service: Running on http://localhost:8084"
else
    echo "  ⏳ Query Service: Starting... (check query-service.log for details)"
fi

echo ""
echo "💡 To start other services:"
echo "  java -jar partition-manager/target/partition-manager-1.0.0.jar"
echo "  java -jar log-consumer/target/log-consumer-1.0.0.jar"
echo "  java -jar log-producer/target/log-producer-1.0.0.jar"
echo ""
echo "📊 Dashboard: http://localhost:3001"

