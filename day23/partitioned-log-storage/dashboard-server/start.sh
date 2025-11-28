#!/bin/bash

echo "📊 Starting Partitioned Log Storage Dashboard..."

# Check if node_modules exists
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies..."
    npm install
fi

echo "Starting dashboard server..."
npm start

