#!/bin/bash

if [ -f /tmp/test-data-generator.pid ]; then
    PID=$(cat /tmp/test-data-generator.pid)
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID
        rm /tmp/test-data-generator.pid
        echo "Test data generator stopped (PID: $PID)"
    else
        echo "Test data generator is not running"
        rm /tmp/test-data-generator.pid
    fi
else
    echo "No test data generator PID file found"
fi

