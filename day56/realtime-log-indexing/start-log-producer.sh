#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f "log-producer/target/log-producer-1.0.0.jar" ]; then
    echo "JAR file not found. Building project..."
    mvn clean install -DskipTests
fi

if [ ! -f "log-producer/target/log-producer-1.0.0.jar" ]; then
    echo "Error: Failed to build log-producer"
    exit 1
fi

echo "Starting Log Producer on port 8081..."
java -jar log-producer/target/log-producer-1.0.0.jar
