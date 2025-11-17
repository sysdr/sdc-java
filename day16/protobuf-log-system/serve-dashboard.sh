#!/bin/bash

# Simple HTTP server to serve the dashboard
# This avoids CORS issues when fetching metrics

PORT=${1:-8083}

# Kill any existing server on this port
EXISTING_PID=$(lsof -ti:$PORT 2>/dev/null || ss -ltnp 2>/dev/null | grep ":$PORT" | awk '{print $NF}' | cut -d',' -f2 | cut -d'=' -f2 | head -1)
if [ -n "$EXISTING_PID" ]; then
    echo "Stopping existing server on port $PORT (PID: $EXISTING_PID)"
    kill $EXISTING_PID 2>/dev/null
    sleep 1
fi

echo "Starting dashboard server on http://localhost:$PORT"
echo "Open http://localhost:$PORT/dashboard.html in your browser"
echo ""
echo "Press Ctrl+C to stop the server"
echo ""

cd "$(dirname "$0")"

# Verify dashboard.html exists
if [ ! -f "dashboard.html" ]; then
    echo "Error: dashboard.html not found in current directory"
    echo "Current directory: $(pwd)"
    exit 1
fi

# Try Python 3 first, then Python 2, then use a simple alternative
if command -v python3 &> /dev/null; then
    python3 -m http.server $PORT
elif command -v python &> /dev/null; then
    python -m SimpleHTTPServer $PORT
else
    echo "Python not found. Please install Python or use a different HTTP server."
    echo "Alternatively, you can open dashboard.html directly in your browser."
    exit 1
fi

