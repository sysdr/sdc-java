#!/bin/bash

echo "Stopping all services..."

if [ -f /tmp/producer.pid ]; then
    PID=$(cat /tmp/producer.pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo "Stopping producer (PID: $PID)..."
        kill $PID
        rm /tmp/producer.pid
    fi
fi

if [ -f /tmp/consumer1.pid ]; then
    PID=$(cat /tmp/consumer1.pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo "Stopping consumer 1 (PID: $PID)..."
        kill $PID
        rm /tmp/consumer1.pid
    fi
fi

if [ -f /tmp/gateway.pid ]; then
    PID=$(cat /tmp/gateway.pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo "Stopping gateway (PID: $PID)..."
        kill $PID
        rm /tmp/gateway.pid
    fi
fi

# Also kill any remaining processes
pkill -f "log-producer" || true
pkill -f "log-consumer" || true
pkill -f "api-gateway" || true

echo "All services stopped."

