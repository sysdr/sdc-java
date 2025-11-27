#!/bin/bash

echo "Starting real-time test data generator..."
echo "This will continuously send syslog and journald messages"
echo ""

# Check if already running
if [ -f /tmp/test-data-generator.pid ]; then
    OLD_PID=$(cat /tmp/test-data-generator.pid)
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo "Test data generator is already running (PID: $OLD_PID)"
        echo "To stop it, run: kill $OLD_PID"
        exit 1
    fi
fi

# Start the generator
nohup bash -c '
count=1
while true; do
    # Send UDP syslog (RFC 3164)
    echo "<$((RANDOM % 192))>$(date "+%b %d %H:%M:%S") test-host-$((RANDOM % 5 + 1)) test-app-$((RANDOM % 3 + 1)): Real-time message #$count" | \
        nc -u -w0 localhost 1514 2>/dev/null
    
    # Send TCP syslog (RFC 5424)
    echo "<$((RANDOM % 192))>1 $(date -u +%Y-%m-%dT%H:%M:%SZ) test-host-$((RANDOM % 5 + 1)) test-app 1234 ID$count - Structured #$count" | \
        nc -w0 localhost 1601 2>/dev/null
    
    # Generate journald messages every 3 iterations
    if [ $((count % 3)) -eq 0 ]; then
        logger -p user.info "Journald test message $count"
        logger -p user.warning "Journald warning $count"
    fi
    
    # Print status every 20 messages
    if [ $((count % 20)) -eq 0 ]; then
        echo "[$(date '+%H:%M:%S')] Sent $count messages..."
    fi
    
    count=$((count + 1))
    sleep 1  # Send 1 message per second
done
' > /tmp/test-data.log 2>&1 &

echo $! > /tmp/test-data-generator.pid
echo "Test data generator started (PID: $(cat /tmp/test-data-generator.pid))"
echo ""
echo "Messages are being sent to:"
echo "  - Syslog UDP: localhost:1514"
echo "  - Syslog TCP: localhost:1601"
echo "  - Journald: via logger command"
echo ""
echo "View metrics at:"
echo "  - Dashboard: http://localhost:8085"
echo "  - Prometheus: http://localhost:9090"
echo ""
echo "To stop the generator, run:"
echo "  kill $(cat /tmp/test-data-generator.pid)"
echo "  or: ./stop-test-data.sh"

