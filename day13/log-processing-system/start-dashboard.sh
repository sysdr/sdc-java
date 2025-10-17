#!/bin/bash

set -e

echo "🚀 Starting Simple Dashboard..."

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo "❌ Node.js is not installed. Please install Node.js 18+ first."
    echo "   Visit: https://nodejs.org/"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "❌ npm is not installed. Please install npm first."
    exit 1
fi

# Navigate to dashboard directory
cd simple-dashboard

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo "📦 Installing dependencies..."
    npm install
fi

# Start the dashboard
echo "🌐 Starting dashboard server on http://localhost:3001"
echo "   Press Ctrl+C to stop"
echo ""

npm start
