#!/bin/bash

# Simple HTTP server to serve the dashboard
# This helps avoid CORS issues when accessing localhost APIs

PORT=${1:-8080}
PROXY_PORT=${2:-8083}

echo "Starting dashboard services..."
echo ""

# Start CORS proxy in background
if command -v python3 &> /dev/null; then
    echo "Starting CORS proxy on port $PROXY_PORT..."
    python3 cors-proxy.py $PROXY_PORT > /dev/null 2>&1 &
    PROXY_PID=$!
    sleep 1
    echo "CORS proxy started (PID: $PROXY_PID)"
    echo ""
elif command -v python &> /dev/null; then
    echo "Starting CORS proxy on port $PROXY_PORT..."
    python cors-proxy.py $PROXY_PORT > /dev/null 2>&1 &
    PROXY_PID=$!
    sleep 1
    echo "CORS proxy started (PID: $PROXY_PID)"
    echo ""
else
    echo "Warning: Python not found. CORS proxy will not start."
    echo "You may encounter CORS issues with Prometheus and Grafana."
    PROXY_PID=""
    echo ""
fi

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "Shutting down services..."
    if [ ! -z "$PROXY_PID" ]; then
        kill $PROXY_PID 2>/dev/null
        echo "CORS proxy stopped"
    fi
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "Starting dashboard server on http://localhost:$PORT"
echo "Open http://localhost:$PORT/dashboard.html in your browser"
echo ""
echo "Press Ctrl+C to stop all services"
echo ""

# Check if Python 3 is available
if command -v python3 &> /dev/null; then
    python3 -m http.server $PORT
elif command -v python &> /dev/null; then
    python -m SimpleHTTPServer $PORT
else
    echo "Error: Python not found. Please install Python to run this server."
    echo "Alternatively, you can open dashboard.html directly in your browser,"
    echo "but you may encounter CORS issues when fetching metrics."
    cleanup
    exit 1
fi

