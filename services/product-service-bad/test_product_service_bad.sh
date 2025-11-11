#!/bin/bash

echo "Testing Product Service Bad (50% error rate)..."
echo ""

echo "1. Health check:"
curl -X GET http://localhost:8081/api/health
echo ""
echo ""

echo "2. Testing product creation (running 10 times to see 503 errors):"
for i in {1..10}; do
  echo "Request $i:"
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8081/api/product \
    -H "Content-Type: application/json" \
    -d '{
      "productId": '$i',
      "sku": "TEST'$i'",
      "manufacturer": "Test Corp",
      "categoryId": 1,
      "weight": 100,
      "someOtherId": 1
    }')
  
  if [ "$STATUS" -eq 201 ]; then
    echo "  ✓ Status: $STATUS (Success)"
  elif [ "$STATUS" -eq 503 ]; then
    echo "  ✗ Status: $STATUS (Service Unavailable - Expected)"
  else
    echo "  ? Status: $STATUS (Unexpected)"
  fi
done

echo ""
echo "Test completed!"

