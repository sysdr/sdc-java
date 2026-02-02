#!/bin/bash

# Startup script for all services
# Checks for existing processes and starts services in background

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "========================================="
echo "Starting Log Search System Services"
echo "========================================="

# Function to check if a process is running on a port
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1 || netstat -tuln 2>/dev/null | grep -q ":$port "; then
        return 0
    fi
    return 1
}

# Function to start a service
start_service() {
    local service_name=$1
    local port=$2
    local service_dir="$SCRIPT_DIR/$service_name"
    
    if [ ! -d "$service_dir" ]; then
        echo "ERROR: Service directory not found: $service_dir"
        return 1
    fi
    
    if check_port $port; then
        echo "WARNING: Port $port is already in use. Service $service_name may already be running."
        echo "Checking for existing Java processes..."
        ps aux | grep -i "$service_name" | grep -v grep || echo "No existing $service_name process found"
        return 1
    fi
    
    echo "Starting $service_name on port $port..."
    cd "$service_dir"
    nohup mvn spring-boot:run > "../${service_name}.log" 2>&1 &
    local pid=$!
    echo "$service_name started with PID: $pid"
    echo $pid > "../${service_name}.pid"
    cd "$SCRIPT_DIR"
    
    # Wait a bit to check if it started successfully
    sleep 3
    if ! ps -p $pid > /dev/null 2>&1; then
        echo "ERROR: $service_name failed to start. Check ${service_name}.log for details."
        return 1
    fi
    
    return 0
}

# Check for duplicate services
echo "Checking for existing services..."
EXISTING_SERVICES=0

if check_port 8081; then
    echo "WARNING: Port 8081 (log-producer) is already in use"
    EXISTING_SERVICES=$((EXISTING_SERVICES + 1))
fi

if check_port 8082; then
    echo "WARNING: Port 8082 (log-indexer) is already in use"
    EXISTING_SERVICES=$((EXISTING_SERVICES + 1))
fi

if check_port 8083; then
    echo "WARNING: Port 8083 (search-api) is already in use"
    EXISTING_SERVICES=$((EXISTING_SERVICES + 1))
fi

if [ $EXISTING_SERVICES -gt 0 ]; then
    echo ""
    echo "Found $EXISTING_SERVICES service(s) already running."
    echo "Do you want to continue? (y/n)"
    read -t 5 -n 1 response || response="n"
    echo ""
    if [ "$response" != "y" ] && [ "$response" != "Y" ]; then
        echo "Aborted. Please stop existing services first."
        exit 1
    fi
fi

# Start services
echo ""
start_service "log-producer" 8081
PRODUCER_STATUS=$?

start_service "log-indexer" 8082
INDEXER_STATUS=$?

start_service "search-api" 8083
SEARCH_STATUS=$?

echo ""
echo "========================================="
if [ $PRODUCER_STATUS -eq 0 ] && [ $INDEXER_STATUS -eq 0 ] && [ $SEARCH_STATUS -eq 0 ]; then
    echo "All services started successfully!"
    echo "========================================="
    echo "Service PIDs:"
    [ -f log-producer.pid ] && echo "  log-producer: $(cat log-producer.pid)"
    [ -f log-indexer.pid ] && echo "  log-indexer: $(cat log-indexer.pid)"
    [ -f search-api.pid ] && echo "  search-api: $(cat search-api.pid)"
    echo ""
    echo "Log files:"
    echo "  log-producer.log"
    echo "  log-indexer.log"
    echo "  search-api.log"
    echo ""
    echo "To stop services, run: ./stop-services.sh"
else
    echo "Some services failed to start. Check logs for details."
    exit 1
fi
echo "========================================="
