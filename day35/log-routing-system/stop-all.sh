#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "Stopping Log Routing System..."

# Stop services by PID if files exist
if [ -d "$SCRIPT_DIR/logs" ]; then
    for pidfile in "$SCRIPT_DIR/logs"/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            service=$(basename "$pidfile" .pid)
            if ps -p "$pid" > /dev/null 2>&1; then
                echo "Stopping $service (PID: $pid)..."
                kill "$pid" 2>/dev/null || true
            fi
        fi
    done
fi

# Also kill any Java processes running Spring Boot apps
echo "Stopping any remaining Spring Boot applications..."
pkill -f "spring-boot:run" || true

# Stop Docker containers
echo "Stopping Docker containers..."
docker-compose down || true

echo "All services stopped!"

