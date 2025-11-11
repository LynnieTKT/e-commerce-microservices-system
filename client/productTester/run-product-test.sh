#!/bin/bash

# Product Test Runner Script

set -e

echo "================================"
echo "Product Test Quick Start"
echo "================================"
echo ""

# Check if JAR exists
if [ ! -f "target/product-tester-1.0.0.jar" ]; then
    echo "JAR file not found. Building..."
    mvn clean package
    echo ""
fi

# Default values
THREADS=${1:-10}
REQUESTS=${2:-1000}
CONFIG=${3:-config/local.properties}

echo "Configuration:"
echo "  Threads:  $THREADS"
echo "  Requests: $REQUESTS"
echo "  Config:   $CONFIG"
echo ""

# Check if services are running (only for local config)
if [[ "$CONFIG" == *"local"* ]]; then
    echo "Checking local product services..."
    if ! curl -s http://localhost:8080/api/health > /dev/null 2>&1; then
        echo "WARNING: Good Product Service (port 8080) may not be running!"
    else
        echo "Good Product Service is running!"
    fi
    
    if ! curl -s http://localhost:8081/api/health > /dev/null 2>&1; then
        echo "WARNING: Bad Product Service (port 8081) may not be running!"
    else
        echo "Bad Product Service is running!"
    fi
    echo ""
fi

# Run the test
echo "Starting product test..."
echo ""

java -jar target/product-tester-1.0.0.jar \
  -t "$THREADS" \
  -n "$REQUESTS" \
  --config "$CONFIG"

echo ""
echo "Test complete!"

