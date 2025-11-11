#!/bin/bash

echo "Testing Product Service..."
echo ""

echo "1. Health check:"
curl -X GET http://localhost:8080/api/health
echo ""
echo ""

echo "2. Creating a product:"
curl -X POST http://localhost:8080/api/product \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "sku": "TEST123",
    "manufacturer": "Test Corp",
    "categoryId": 1,
    "weight": 100,
    "someOtherId": 1
  }' -v
echo ""
echo ""

echo "Test completed!"

