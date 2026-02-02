#!/bin/bash

# Stop script for all services

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "Stopping Log Search System Services"
echo "========================================="

stop_service() {
    local service_name=$1
    local pid_file="${SCRIPT_DIR}/${service_name}.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        if ps -p $pid > /dev/null 2>&1; then
            echo "Stopping $service_name (PID: $pid)..."
            kill $pid
            sleep 2
            if ps -p $pid > /dev/null 2>&1; then
                echo "Force killing $service_name..."
                kill -9 $pid
            fi
            echo "$service_name stopped."
        else
            echo "$service_name was not running."
        fi
        rm -f "$pid_file"
    else
        echo "PID file not found for $service_name. Trying to find process..."
        pkill -f "$service_name.*spring-boot:run" && echo "$service_name stopped." || echo "$service_name not found."
    fi
}

stop_service "log-producer"
stop_service "log-indexer"
stop_service "search-api"

echo ""
echo "========================================="
echo "All services stopped"
echo "========================================="
