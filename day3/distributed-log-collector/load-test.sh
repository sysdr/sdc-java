#!/bin/bash

echo "🔥 Running load tests..."

# Increase log generation rate
echo "Increasing log generation rate..."
curl -X POST "http://localhost:8081/actuator/refresh" || true

# Monitor system during load
echo "Monitoring system performance..."
for i in {1..10}; do
    echo "Test iteration $i/10"
    
    # Make multiple requests to gateway
    for j in {1..10}; do
        curl -s http://localhost:8080/api/system/stats > /dev/null &
    done
    
    wait
    
    # Check system stats
    stats=$(curl -s http://localhost:8080/api/system/stats)
    echo "Current stats: $stats"
    
    sleep 5
done

echo "✅ Load test completed!"
echo ""
echo "📈 View detailed metrics at:"
echo "  • Grafana: http://localhost:3000"
echo "  • Prometheus: http://localhost:9090"
