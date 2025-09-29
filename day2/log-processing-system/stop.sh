#!/bin/bash

echo "🛑 Stopping log processing system..."

# Stop Java applications
if [ -f log-generator.pid ]; then
    LOG_GENERATOR_PID=$(cat log-generator.pid)
    kill $LOG_GENERATOR_PID 2>/dev/null || echo "Log Generator already stopped"
    rm log-generator.pid
fi

if [ -f api-gateway.pid ]; then
    API_GATEWAY_PID=$(cat api-gateway.pid)
    kill $API_GATEWAY_PID 2>/dev/null || echo "API Gateway already stopped"
    rm api-gateway.pid
fi

# Stop Docker services
echo "🐳 Stopping Docker services..."
docker-compose down

echo "✅ System stopped!"
