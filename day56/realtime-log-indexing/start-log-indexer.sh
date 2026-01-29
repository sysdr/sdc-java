#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f "log-indexer/target/log-indexer-1.0.0.jar" ]; then
    echo "JAR file not found. Building project..."
    mvn clean install -DskipTests
fi

if [ ! -f "log-indexer/target/log-indexer-1.0.0.jar" ]; then
    echo "Error: Failed to build log-indexer"
    exit 1
fi

echo "Starting Log Indexer on port 8082..."
java -jar log-indexer/target/log-indexer-1.0.0.jar
