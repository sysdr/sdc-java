#!/bin/bash

echo "Running load test on syslog adapter..."

# Generate 10000 syslog messages
for i in {1..10000}; do
    echo "<34>Oct 11 22:14:15 host$i app: Load test message $i" | \
        nc -u -w0 localhost 514
    
    if [ $((i % 1000)) -eq 0 ]; then
        echo "Sent $i messages..."
    fi
done

echo "Load test complete!"
echo "View metrics at:"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000"
echo ""
echo "Check processing stats:"
curl -s "http://localhost:8080/api/logs/stats" | jq '.'
