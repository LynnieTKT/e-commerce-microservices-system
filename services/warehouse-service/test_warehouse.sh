#!/bin/bash

echo "================================"
echo "Warehouse Service Test"
echo "================================"

# Check if warehouse service is running
echo ""
echo "1. Checking warehouse service health"
HEALTH_RESPONSE=$(curl -s http://localhost:8084/actuator/health)

if echo "$HEALTH_RESPONSE" | grep -q "UP"; then
    echo "   ✓ Warehouse service is UP and running"
else
    echo "   ✗ Warehouse service is not responding"
    exit 1
fi

# Check RabbitMQ connection
echo ""
echo "2. Checking RabbitMQ queue"
QUEUE_INFO=$(curl -s -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue)

if echo "$QUEUE_INFO" | grep -q "warehouse-orders-queue"; then
    MESSAGES=$(echo $QUEUE_INFO | grep -o '"messages":[0-9]*' | grep -o '[0-9]*')
    CONSUMERS=$(echo $QUEUE_INFO | grep -o '"consumers":[0-9]*' | grep -o '[0-9]*')
    echo "   ✓ Queue 'warehouse-orders-queue' exists"
    echo "   - Messages in queue: $MESSAGES"
    echo "   - Active consumers: $CONSUMERS"
else
    echo "   ✗ Queue 'warehouse-orders-queue' not found"
    exit 1
fi

# Send test messages via shopping cart service
echo ""
echo "3. Sending test orders through shopping cart service"

for i in {1..5}; do
    # Create cart
    CART_RESPONSE=$(curl -s -X POST http://localhost:8083/shopping-cart \
      -H "Content-Type: application/json" \
      -d "{\"customer_id\": $((100 + i))}")
    
    CART_ID=$(echo $CART_RESPONSE | grep -o '"shopping_cart_id":[0-9]*' | grep -o '[0-9]*')
    
    if [ -z "$CART_ID" ]; then
        echo "   ✗ Failed to create cart $i"
        continue
    fi
    
    # Add items
    curl -s -X POST http://localhost:8083/shopping-carts/$CART_ID/addItem \
      -H "Content-Type: application/json" \
      -d "{\"product_id\": $((i * 10)), \"quantity\": $i}" > /dev/null
    
    # Checkout
    CHECKOUT_RESPONSE=$(curl -s -X POST http://localhost:8083/shopping-carts/$CART_ID/checkout \
      -H "Content-Type: application/json" \
      -d '{"credit_card_number": "1234-5678-9012-3456"}')
    
    if echo "$CHECKOUT_RESPONSE" | grep -q "order_id"; then
        ORDER_ID=$(echo $CHECKOUT_RESPONSE | grep -o '"order_id":[0-9]*' | grep -o '[0-9]*')
        echo "   ✓ Order $i created (ID: $ORDER_ID)"
    else
        echo "   ⚠ Order $i failed (possibly payment declined - 10% chance)"
    fi
done

echo ""
echo "4. Waiting for warehouse to process messages..."
sleep 3

# Check queue again
QUEUE_INFO_AFTER=$(curl -s -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue)
MESSAGES_AFTER=$(echo $QUEUE_INFO_AFTER | grep -o '"messages":[0-9]*' | grep -o '[0-9]*')

echo "   Messages remaining in queue: $MESSAGES_AFTER"

if [ "$MESSAGES_AFTER" = "0" ]; then
    echo "   ✓ All messages processed!"
else
    echo "   ⚠ Some messages still in queue (warehouse might be processing)"
fi

echo ""
echo "================================"
echo "Test Complete"
echo "================================"
echo ""
echo "To view warehouse statistics and logs:"
echo "  docker logs warehouse-service"
echo ""
echo "To stop and see final statistics:"
echo "  docker stop warehouse-service"
echo ""

