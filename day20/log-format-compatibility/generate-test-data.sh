#!/bin/bash

echo "Generating real-time test log data..."
echo "This will send continuous syslog and journald messages"
echo "Press Ctrl+C to stop"
echo ""

# Function to send syslog messages
send_syslog() {
    local count=$1
    local severity=$2
    local facility=$3
    local priority=$((facility * 8 + severity))
    local hostname="test-host-$(($RANDOM % 5 + 1))"
    local app="test-app-$((RANDOM % 3 + 1))"
    
    # RFC 3164 format
    echo "<${priority}>$(date '+%b %d %H:%M:%S') ${hostname} ${app}: Test message #${count} - $(date)" | \
        nc -u -w0 localhost 1514 2>/dev/null || echo "Failed to send UDP syslog"
    
    # RFC 5424 format (TCP)
    echo "<${priority}>1 $(date -u +%Y-%m-%dT%H:%M:%SZ) ${hostname} ${app} 1234 ID${count} - Test structured message #${count}" | \
        nc -w0 localhost 1601 2>/dev/null || echo "Failed to send TCP syslog"
}

# Function to generate journald-like messages via systemd
generate_journald() {
    # Use logger to generate systemd journal entries
    logger -p user.info "Test journald message $(date +%s) - Generated for metrics testing"
    logger -p user.warning "Test warning message $(date +%s)"
    logger -p user.err "Test error message $(date +%s)"
}

count=1
while true; do
    # Send syslog messages
    send_syslog $count $((RANDOM % 8)) $((RANDOM % 24))
    
    # Generate journald messages every 3 iterations
    if [ $((count % 3)) -eq 0 ]; then
        generate_journald
    fi
    
    # Print progress every 10 messages
    if [ $((count % 10)) -eq 0 ]; then
        echo "Sent $count messages... ($(date '+%H:%M:%S'))"
        # Show current metrics
        echo "Current metrics:"
        curl -s http://localhost:8080/actuator/prometheus 2>/dev/null | \
            grep -E "syslog_messages_produced_total|journald_messages_produced_total|normalizer_events_processed_total" | \
            head -3 || echo "  (metrics not available yet)"
        echo ""
    fi
    
    count=$((count + 1))
    sleep 0.5  # Send every 0.5 seconds for real-time data
done

