#!/bin/bash

echo "🚀 Starting All Partitioned Log Storage Services..."
echo ""

# Check if JARs exist
if [ ! -f "query-service/target/query-service-1.0.0.jar" ]; then
    echo "❌ JAR files not found. Please run './setup.sh' first to build the services."
    exit 1
fi

# Function to start a service
start_service() {
    local service_name=$1
    local jar_path=$2
    local port=$3
    
    if lsof -i :$port > /dev/null 2>&1; then
        echo "  ⚠️  $service_name is already running on port $port"
        return 1
    else
        nohup java -jar "$jar_path" > "${service_name,,}.log" 2>&1 &
        local pid=$!
        echo "  ✅ $service_name started (PID: $pid, Port: $port)"
        echo "  📝 Logs: ${service_name,,}.log"
        return 0
    fi
}

# Start services in order
echo "Starting services..."
echo ""

# 1. Partition Manager (must start first)
start_service "Partition Manager" "partition-manager/target/partition-manager-1.0.0.jar" 8083
sleep 2

# 2. Log Consumer
start_service "Log Consumer" "log-consumer/target/log-consumer-1.0.0.jar" 8082
sleep 2

# 3. Log Producer
start_service "Log Producer" "log-producer/target/log-producer-1.0.0.jar" 8081
sleep 2

# 4. Query Service
start_service "Query Service" "query-service/target/query-service-1.0.0.jar" 8084
sleep 3

# Check status
echo ""
echo "═══════════════════════════════════════════════════════════"
echo "Service Status:"
echo "═══════════════════════════════════════════════════════════"

check_service() {
    local name=$1
    local url=$2
    if curl -s "$url" > /dev/null 2>&1; then
        echo "  ✅ $name: Running"
    else
        echo "  ⏳ $name: Starting... (check logs for details)"
    fi
}

check_service "Partition Manager" "http://localhost:8083/actuator/health"
check_service "Log Consumer" "http://localhost:8082/actuator/health"
check_service "Log Producer" "http://localhost:8081/api/logs/health"
check_service "Query Service" "http://localhost:8084/api/query/health"

echo ""
echo "═══════════════════════════════════════════════════════════"
echo "📊 Dashboard: http://localhost:3001"
echo "📈 Grafana: http://localhost:3000 (admin/admin)"
echo "🔍 Prometheus: http://localhost:9090"
echo ""
echo "💡 To stop all services:"
echo "   pkill -f 'java -jar.*target.*jar'"
echo ""
echo "💡 To view logs:"
echo "   tail -f partition-manager.log"
echo "   tail -f log-consumer.log"
echo "   tail -f log-producer.log"
echo "   tail -f query-service.log"
echo "═══════════════════════════════════════════════════════════"

