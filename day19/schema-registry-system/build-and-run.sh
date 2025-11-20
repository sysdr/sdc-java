#!/bin/bash
set -e

echo "Building Schema Registry System..."

# Build all modules
mvn clean package -DskipTests

echo ""
echo "Build complete! To run:"
echo ""
echo "1. Start infrastructure:"
echo "   ./setup.sh"
echo ""
echo "2. In separate terminals, start services:"
echo "   java -jar schema-registry/target/schema-registry-1.0.0-SNAPSHOT.jar"
echo "   java -jar validation-gateway/target/validation-gateway-1.0.0-SNAPSHOT.jar"
