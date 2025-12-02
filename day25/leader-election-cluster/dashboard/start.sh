#!/bin/bash

echo "🚀 Starting Leader Election Cluster Dashboard..."
echo ""
echo "📊 Dashboard will be available at: http://localhost:8000"
echo ""

# Check if Python 3 is available
if command -v python3 &> /dev/null; then
    python3 server.py
elif command -v python &> /dev/null; then
    python server.py
else
    echo "❌ Error: Python 3 is required but not found"
    echo "Please install Python 3 to run the dashboard server"
    exit 1
fi

