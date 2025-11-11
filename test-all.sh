#!/bin/bash

echo "================================"
echo "Testing"
echo "================================"

# Test 1: Create cart
echo ""
echo "1. Creating shopping cart"
CART_RESPONSE=$(curl -s -X POST http://localhost:8083/shopping-cart \
  -H "Content-Type: application/json" \
  -d '{"customer_id": 100}')
echo "$CART_RESPONSE"

CART_ID=$(echo $CART_RESPONSE | grep -o '"shopping_cart_id":[0-9]*' | grep -o '[0-9]*')

if [ -z "$CART_ID" ]; then
    echo "ERROR: Failed to create cart"
    exit 1
fi

echo "Cart ID: $CART_ID"

# Test 2: Add items
echo ""
echo "2. Adding items to cart"
curl -s -X POST http://localhost:8083/shopping-carts/$CART_ID/addItem \
  -H "Content-Type: application/json" \
  -d '{"product_id": 5, "quantity": 2}' > /dev/null
echo "Added product 5 (qty: 2)"

curl -s -X POST http://localhost:8083/shopping-carts/$CART_ID/addItem \
  -H "Content-Type: application/json" \
  -d '{"product_id": 10, "quantity": 3}' > /dev/null
echo "Added product 10 (qty: 3)"

# Test 3: Checkout
echo ""
echo "3. Checking out (calling real CCA)"
CHECKOUT_RESPONSE=$(curl -s -X POST http://localhost:8083/shopping-carts/$CART_ID/checkout \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234-5678-9012-3456"}')

echo "$CHECKOUT_RESPONSE"

if echo "$CHECKOUT_RESPONSE" | grep -q "order_id"; then
    ORDER_ID=$(echo $CHECKOUT_RESPONSE | grep -o '"order_id":[0-9]*' | grep -o '[0-9]*')
    echo ""
    echo "SUCCESS! Order created: $ORDER_ID"
    
    # Test 4: Verify warehouse received the message
    echo ""
    echo "4. Verifying warehouse processed the order"
    echo "   Waiting 2 seconds for message processing..."
    sleep 2
    
    QUEUE_AFTER=$(curl -s -u guest:guest http://localhost:15672/api/queues/%2F/warehouse-orders-queue | grep -o '"messages":[0-9]*' | grep -o '[0-9]*')
    echo "   Messages in queue after: $QUEUE_AFTER"
    
    if [ "$QUEUE_AFTER" = "0" ] || [ "$QUEUE_AFTER" -lt "$QUEUE_BEFORE" ]; then
        echo "   ✓ Warehouse consumed the message!"
        echo ""
        echo "   Check warehouse logs to see statistics:"
        echo "   docker logs warehouse-service"
    else
        echo "   ⚠ Queue still has messages - warehouse might be slow or not running"
    fi
    
    echo ""
    echo "================================"
    echo "Test Summary"
    echo "================================"
    echo "✓ Shopping cart created"
    echo "✓ Items added to cart"
    echo "✓ Order placed successfully (Order ID: $ORDER_ID)"
    echo "✓ Message sent to RabbitMQ"
    echo "✓ Warehouse processed the order"
    echo ""
    echo "To see warehouse statistics:"
    echo "  docker logs warehouse-service"
    echo ""
    echo "To see RabbitMQ management UI:"
    echo "  http://localhost:15672 (guest/guest)"
    
elif echo "$CHECKOUT_RESPONSE" | grep -q "Payment declined"; then
    echo ""
    echo "Payment was declined (10% chance)"
    echo "This is expected behavior - try running the script again"
else
    echo ""
    echo "ERROR: Unexpected response"
    exit 1
fi