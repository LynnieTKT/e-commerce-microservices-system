#!/bin/bash

echo "Testing Shopping Cart Service..."
echo ""

# Base URL
BASE_URL="http://localhost:8081"

echo "1. Health check:"
curl -X GET $BASE_URL/actuator/health
echo ""
echo ""

echo "2. Creating a shopping cart:"
CART_RESPONSE=$(curl -s -X POST $BASE_URL/shopping-cart \
  -H "Content-Type: application/json" \
  -d '{"customer_id": 100}')
echo $CART_RESPONSE

# Extract cart ID
CART_ID=$(echo $CART_RESPONSE | grep -o '"shopping_cart_id":[0-9]*' | grep -o '[0-9]*')
echo "Created cart ID: $CART_ID"
echo ""
echo ""

echo "3. Adding item to cart (product 5, quantity 2):"
ADD_ITEM_1=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/shopping-carts/$CART_ID/addItem \
  -H "Content-Type: application/json" \
  -d '{"product_id": 5, "quantity": 2}')
if [ $ADD_ITEM_1 -eq 204 ]; then
    echo "✓ Successfully added (HTTP $ADD_ITEM_1)"
else
    echo "✗ Failed to add item (HTTP $ADD_ITEM_1)"
fi
echo ""

echo "4. Adding another item (product 10, quantity 3):"
ADD_ITEM_2=$(curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/shopping-carts/$CART_ID/addItem \
  -H "Content-Type: application/json" \
  -d '{"product_id": 10, "quantity": 3}')
if [ $ADD_ITEM_2 -eq 204 ]; then
    echo "✓ Successfully added (HTTP $ADD_ITEM_2)"
else
    echo "✗ Failed to add item (HTTP $ADD_ITEM_2)"
fi
echo ""

echo "5. Checkout (will randomly authorize/decline):"
CHECKOUT_RESPONSE=$(curl -s -X POST $BASE_URL/shopping-carts/$CART_ID/checkout \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234-5678-9012-3456"}')
echo $CHECKOUT_RESPONSE
echo ""
echo ""

echo "6. Testing invalid credit card format:"
curl -s -X POST $BASE_URL/shopping-carts/1/checkout \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "invalid-card"}'
echo ""
echo ""

echo "7. Testing cart not found error:"
curl -s -X POST $BASE_URL/shopping-carts/99999/addItem \
  -H "Content-Type: application/json" \
  -d '{"product_id": 1, "quantity": 1}'
echo ""
echo ""

echo "Test completed!"
echo ""
echo "Check RabbitMQ Management UI at http://localhost:15672 (guest/guest)"
echo "Queue 'warehouse-orders-queue' should contain messages"