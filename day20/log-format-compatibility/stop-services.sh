#!/bin/bash

echo "Stopping Log Format Compatibility Layer Services..."

if [ -d "logs" ]; then
    for pidfile in logs/*.pid; do
        if [ -f "$pidfile" ]; then
            pid=$(cat "$pidfile")
            name=$(basename "$pidfile" .pid)
            if kill -0 "$pid" 2>/dev/null; then
                echo "Stopping $name (PID: $pid)..."
                kill "$pid"
                rm "$pidfile"
            fi
        fi
    done
    echo "All services stopped."
else
    echo "No services running (logs directory not found)."
fi

