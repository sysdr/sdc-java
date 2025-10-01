#!/bin/bash

echo "⚡ Running load tests..."

# Function to generate random log data
generate_log() {
    local level=$1
    local source=$2
    local message=$3
    
    cat << EOF
{
  "level": "$level",
  "source": "$source", 
  "message": "$message $(date +%s)",
  "metadata": {
    "user_id": "$((RANDOM % 1000))",
    "session_id": "session_$((RANDOM % 100))"
  }
}
EOF
}

# Test data arrays
levels=("INFO" "WARN" "ERROR" "DEBUG")
sources=("auth-service" "user-service" "payment-service" "notification-service")
messages=("User login successful" "Payment processed" "Database connection failed" "Cache miss" "API rate limit exceeded")

# Generate and send test logs
echo "Generating test logs..."

for i in {1..100}; do
    level=${levels[$((RANDOM % ${#levels[@]}))]}
    source=${sources[$((RANDOM % ${#sources[@]}))]}
    message=${messages[$((RANDOM % ${#messages[@]}))]}
    
    log_data=$(generate_log "$level" "$source" "$message")
    
    # Send to log producer endpoint
    curl -X POST http://localhost:8080/api/logs \
         -H "Content-Type: application/json" \
         -d "$log_data" \
         --silent --output /dev/null
    
    if [ $((i % 10)) -eq 0 ]; then
        echo "Sent $i logs..."
    fi
done

echo "✅ Load test completed! Sent 100 test logs."
