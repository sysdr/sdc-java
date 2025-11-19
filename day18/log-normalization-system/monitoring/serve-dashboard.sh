#!/bin/bash

# Simple HTTP server to serve the dashboard
# This allows the dashboard to access Prometheus and service endpoints without CORS issues

PORT=${1:-8083}

echo "Starting dashboard server on http://localhost:$PORT"
echo "Open http://localhost:$PORT/dashboard.html in your browser"
echo "Press Ctrl+C to stop"

cd "$(dirname "$0")"
python3 -m http.server $PORT 2>/dev/null || python -m SimpleHTTPServer $PORT 2>/dev/null

