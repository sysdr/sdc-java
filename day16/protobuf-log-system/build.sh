#!/bin/bash

# Build script for protobuf-log-system
# This script checks for JDK and builds the project

set -e

echo "Checking for JDK..."

# Check if javac is available
if ! command -v javac &> /dev/null; then
    echo "❌ ERROR: JDK (javac compiler) is not installed!"
    echo ""
    echo "The Java Development Kit (JDK) is required to compile Java code."
    echo "Currently, only the Java Runtime Environment (JRE) is installed."
    echo ""
    echo "To fix this, run:"
    echo "  sudo apt-get update"
    echo "  sudo apt-get install -y openjdk-21-jdk"
    echo ""
    echo "After installation, verify with:"
    echo "  javac -version"
    echo ""
    exit 1
fi

# Verify javac works
JAVAC_VERSION=$(javac -version 2>&1)
echo "✅ Found JDK: $JAVAC_VERSION"

# Set JAVA_HOME if not set
if [ -z "$JAVA_HOME" ]; then
    JAVA_PATH=$(which java)
    if [ -n "$JAVA_PATH" ]; then
        JAVA_HOME=$(dirname $(dirname $(readlink -f "$JAVA_PATH")))
        export JAVA_HOME
        echo "Set JAVA_HOME to: $JAVA_HOME"
    fi
fi

# Build the project
echo ""
echo "Building project with Maven..."
cd "$(dirname "$0")"
mvn clean install

echo ""
echo "✅ Build completed successfully!"
echo ""
echo "To start the services, run:"
echo "  java -jar log-producer/target/log-producer-1.0.0.jar &"
echo "  java -jar log-consumer/target/log-consumer-1.0.0.jar &"
echo "  java -jar api-gateway/target/api-gateway-1.0.0.jar &"

