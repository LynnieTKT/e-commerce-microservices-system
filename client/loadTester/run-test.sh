#!/bin/bash

# Quick start script for load testing

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

echo "Configuration:"
echo "  Threads:  $THREADS"
echo "  Requests: $REQUESTS"
echo "  Mode:     $MODE"
echo ""

# Check if services are running
echo "Checking services..."
if ! curl -s http://localhost:8083/actuator/health > /dev/null 2>&1; then
    echo "ERROR: Shopping Cart Service is not running!"
    echo "Start services with: docker-compose up -d"
    exit 1
fi

if ! curl -s http://localhost:8084/actuator/health > /dev/null 2>&1; then
    echo "ERROR: Warehouse Service is not running!"
    echo "Start services with: docker-compose up -d"
    exit 1
fi

if ! curl -s http://localhost:15672 > /dev/null 2>&1; then
    echo "ERROR: RabbitMQ Management is not accessible!"
    echo "Start services with: docker-compose up -d"
    exit 1
fi

echo "All services are running!"
echo ""

# Run the test
echo "Starting load test..."
echo ""

java -jar target/load-test-client-1.0.0.jar \
  --threads "$THREADS" \
  --requests "$REQUESTS" \
  --mode "$MODE"

echo ""
echo "Test complete!"
echo ""
echo "To view RabbitMQ statistics:"
echo "  open http://localhost:15672"
echo ""
echo "To view service logs:"
echo "  docker logs shopping-cart-service"
echo "  docker logs warehouse-service"
echo ""

