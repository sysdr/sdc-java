#!/bin/bash

echo "🛑 Shutting down Distributed Log Processing System..."

# Kill application processes
if [ -f logs/consumer.pid ]; then
    kill $(cat logs/consumer.pid) 2>/dev/null || true
    rm logs/consumer.pid
fi

if [ -f logs/producer.pid ]; then
    kill $(cat logs/producer.pid) 2>/dev/null || true
    rm logs/producer.pid
fi

if [ -f logs/gateway.pid ]; then
    kill $(cat logs/gateway.pid) 2>/dev/null || true
    rm logs/gateway.pid
fi

# Stop Docker services
docker-compose down

echo "✅ System shutdown complete!"
