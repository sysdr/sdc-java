#!/bin/bash

echo "🔄 Restarting all services..."

cd "$(dirname "$0")"

# Kill existing services
echo "Stopping existing services..."
pkill -f "dashboard-service.*jar" 2>/dev/null
pkill -f "enrichment-service.*jar" 2>/dev/null
pkill -f "metadata-service.*jar" 2>/dev/null
pkill -f "log-producer.*jar" 2>/dev/null
sleep 3

# Start enrichment service
echo "Starting enrichment-service..."
nohup java -jar enrichment-service/target/enrichment-service-1.0.0.jar > /tmp/enrichment-service.log 2>&1 &
sleep 5

# Start dashboard service
echo "Starting dashboard-service..."
nohup java -jar dashboard-service/target/dashboard-service-1.0.0.jar > /tmp/dashboard-service.log 2>&1 &
sleep 5

# Check status
echo ""
echo "Checking service status..."
sleep 3

if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ enrichment-service: UP"
else
    echo "❌ enrichment-service: DOWN"
fi

if curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
    echo "✅ dashboard-service: UP"
    echo ""
    echo "🌐 Dashboard available at: http://localhost:8083"
else
    echo "❌ dashboard-service: DOWN"
fi

echo ""
echo "✅ Services restarted!"

