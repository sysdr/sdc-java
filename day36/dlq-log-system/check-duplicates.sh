#!/bin/bash

echo "=== Checking for Duplicate Services ==="

check_duplicates() {
    local pattern=$1
    local name=$2
    local count=$(pgrep -f "$pattern" | wc -l)
    
    if [ "$count" -eq 0 ]; then
        echo "$name: Not running"
    elif [ "$count" -eq 1 ]; then
        echo "$name: Running (1 instance) - OK"
        pgrep -f "$pattern" | head -1 | xargs ps -p
    else
        echo "WARNING: $name has $count instances running!"
        pgrep -f "$pattern" | xargs ps -p
    fi
    echo ""
}

check_duplicates "log-producer.*spring-boot:run" "Log Producer"
check_duplicates "log-consumer.*spring-boot:run" "Log Consumer"
check_duplicates "api-gateway.*spring-boot:run" "API Gateway"

# Check ports
echo "=== Checking Ports ==="
for port in 8080 8081 8082; do
    count=$(lsof -ti:$port 2>/dev/null | wc -l)
    if [ "$count" -gt 1 ]; then
        echo "WARNING: Port $port has $count processes!"
        lsof -ti:$port | xargs ps -p
    elif [ "$count" -eq 1 ]; then
        echo "Port $port: OK (1 process)"
    else
        echo "Port $port: Not in use"
    fi
done

