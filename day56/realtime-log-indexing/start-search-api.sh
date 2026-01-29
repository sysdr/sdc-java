#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ ! -f "search-api/target/search-api-1.0.0.jar" ]; then
    echo "JAR file not found. Building project..."
    mvn clean install -DskipTests
fi

if [ ! -f "search-api/target/search-api-1.0.0.jar" ]; then
    echo "Error: Failed to build search-api"
    exit 1
fi

echo "Starting Search API on port 8083..."
java -jar search-api/target/search-api-1.0.0.jar
