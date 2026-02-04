#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/log-consumer"

echo "Starting Log Consumer..."
echo "Working directory: $(pwd)"

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Build if needed
if [ ! -d "target" ] || [ "target/classes" -ot "src" ]; then
    echo "Building Log Consumer..."
    mvn clean install -DskipTests
fi

# Check if already running
if lsof -Pi :8081 -sTCP:LISTEN -t >/dev/null 2>&1 ; then
    echo "Warning: Port 8081 is already in use. Checking if it's our service..."
    PID=$(lsof -ti :8081)
    if ps -p $PID > /dev/null 2>&1; then
        echo "Service already running on port 8081 (PID: $PID)"
        exit 0
    fi
fi

echo "Starting Log Consumer on port 8081..."
mvn spring-boot:run
