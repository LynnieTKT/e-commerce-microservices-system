#!/bin/bash

# Load Test Runner Script

set -e

echo "================================"
echo "Load Test Quick Start"
echo "================================"
echo ""

# Check if JAR exists
if [ ! -f "target/load-test-client-1.0.0.jar" ]; then
    echo "JAR file not found. Building..."
    mvn clean package
    echo ""
fi

# Default values
THREADS=${1:-10}
REQUESTS=${2:-10000}
MODE=${3:-MASS_CHECKOUT}
CONFIG=${4:-config/local.properties}

echo "Configuration:"
echo "  Threads:  $THREADS"
echo "  Requests: $REQUESTS"
echo "  Mode:     $MODE"
echo "  Config:   $CONFIG"
echo ""

# Check if services are running (only for local config)
if [[ "$CONFIG" == *"local"* ]]; then
    echo "Checking local services..."
    if ! curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
        echo "WARNING: Shopping Cart Service may not be running!"
    else
        echo "Shopping Cart Service is running!"
    fi
    
    if ! curl -s http://localhost:8084/actuator/health > /dev/null 2>&1; then
        echo "WARNING: Warehouse Service may not be running!"
    else
        echo "Warehouse Service is running!"
    fi
    echo ""
fi

# Run the test
echo "Starting load test..."
echo ""

java -jar target/load-test-client-1.0.0.jar \
  -t "$THREADS" \
  -n "$REQUESTS" \
  -m "$MODE" \
  --config "$CONFIG"

echo ""
echo "Test complete!"

