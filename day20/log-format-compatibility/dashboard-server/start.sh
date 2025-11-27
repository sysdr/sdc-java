#!/bin/bash

cd "$(dirname "$0")"

echo "Starting Dashboard Server..."

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

echo "Starting server on http://localhost:8085"
npm start

