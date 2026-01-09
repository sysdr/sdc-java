#!/bin/bash

echo "Building services..."
mvn clean package -DskipTests

echo "Starting services in separate terminals..."
echo "Note: Run each in a separate terminal window:"
echo ""
echo "Terminal 1 - Producer:"
echo "cd log-producer && mvn spring-boot:run"
echo ""
echo "Terminal 2 - Consumer Instance 1:"
echo "cd log-consumer && SERVER_PORT=8082 mvn spring-boot:run"
echo ""
echo "Terminal 3 - Consumer Instance 2:"
echo "cd log-consumer && SERVER_PORT=8083 mvn spring-boot:run"
echo ""
echo "Terminal 4 - Gateway:"
echo "cd api-gateway && mvn spring-boot:run"
