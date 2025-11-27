#!/bin/bash

echo "🚀 Starting Log Enrichment System Dashboard..."

# Check if already running
if curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
    echo "✅ Dashboard is already running on http://localhost:8083"
    exit 0
fi

# Navigate to project directory
cd "$(dirname "$0")"

# Check if JAR exists
if [ ! -f "dashboard-service/target/dashboard-service-1.0.0.jar" ]; then
    echo "❌ Dashboard JAR not found. Building..."
    mvn clean package -DskipTests -pl dashboard-service -q
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
fi

# Start the dashboard service
echo "📊 Starting dashboard service on port 8083..."
nohup java -jar dashboard-service/target/dashboard-service-1.0.0.jar > /tmp/dashboard-service.log 2>&1 &

# Wait for service to start
echo "⏳ Waiting for service to start..."
for i in {1..30}; do
    if curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
        echo ""
        echo "✅ Dashboard service started successfully!"
        echo "🌐 Access the dashboard at: http://localhost:8083"
        echo "📋 Logs: tail -f /tmp/dashboard-service.log"
        exit 0
    fi
    sleep 1
    echo -n "."
done

echo ""
echo "❌ Dashboard service failed to start. Check logs:"
echo "   tail -f /tmp/dashboard-service.log"
exit 1

