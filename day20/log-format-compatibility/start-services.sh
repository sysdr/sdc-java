#!/bin/bash

echo "Starting Log Format Compatibility Layer Services..."
echo ""

# Check if JAR files exist
if [ ! -f "syslog-adapter/target/syslog-adapter-1.0.0.jar" ]; then
    echo "Error: JAR files not found. Please build the services first:"
    echo "  mvn clean install"
    exit 1
fi

# Function to start a service in background
start_service() {
    local name=$1
    local jar=$2
    local port=$3
    
    echo "Starting $name on port $port..."
    nohup java -jar "$jar" > "logs/${name}.log" 2>&1 &
    echo $! > "logs/${name}.pid"
    echo "  PID: $(cat logs/${name}.pid)"
    echo "  Log: logs/${name}.log"
    sleep 2
    
    # Check if service started
    if curl -s "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
        echo "  ✓ $name is running"
    else
        echo "  ⚠ $name may still be starting..."
    fi
    echo ""
}

# Create logs directory
mkdir -p logs

# Start services
start_service "syslog-adapter" "syslog-adapter/target/syslog-adapter-1.0.0.jar" "8081"
start_service "journald-adapter" "journald-adapter/target/journald-adapter-1.0.0.jar" "8082"
start_service "format-normalizer" "format-normalizer/target/format-normalizer-1.0.0.jar" "8083"
start_service "api-gateway" "api-gateway/target/api-gateway-1.0.0.jar" "8080"

echo "All services started!"
echo ""
echo "View logs:"
echo "  tail -f logs/syslog-adapter.log"
echo "  tail -f logs/journald-adapter.log"
echo "  tail -f logs/format-normalizer.log"
echo "  tail -f logs/api-gateway.log"
echo ""
echo "Stop services:"
echo "  ./stop-services.sh"
echo ""
echo "Dashboard: http://localhost:8085"

