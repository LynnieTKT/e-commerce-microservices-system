#!/bin/bash

# Test script for Credit Card Authorizer Service
# Usage: ./test_cca.sh [host:port]
# Default: localhost:8082

HOST="${1:-localhost:8082}"
BASE_URL="http://${HOST}/credit-card-authorizer"

echo "========================================="
echo "Testing Credit Card Authorizer Service"
echo "Base URL: $BASE_URL"
echo "========================================="

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test 1: Health Check
echo -e "\n${YELLOW}Test 1: Health Check${NC}"
response=$(curl -s -w "\n%{http_code}" "${BASE_URL}/health")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" == "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Health check returned 200"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 200, got $http_code"
fi

# Test 2: Valid Credit Card (should return 200 or 402)
echo -e "\n${YELLOW}Test 2: Valid Credit Card Format${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234-5678-9012-3456"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" == "200" ] || [ "$http_code" == "402" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Valid format processed (HTTP $http_code)"
    if [ "$http_code" == "200" ]; then
        echo "Result: AUTHORIZED"
    else
        echo "Result: DECLINED"
        echo "Response: $body"
    fi
else
    echo -e "${RED}✗ FAIL${NC} - Expected 200 or 402, got $http_code"
    echo "Response: $body"
fi

# Test 3: Invalid Format - No Dashes
echo -e "\n${YELLOW}Test 3: Invalid Format (No Dashes)${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234567890123456"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" == "400" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Invalid format rejected with 400"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 400, got $http_code"
    echo "Response: $body"
fi

# Test 4: Invalid Format - Wrong Number of Digits
echo -e "\n${YELLOW}Test 4: Invalid Format (Wrong Length)${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234-5678-9012-345"}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" == "400" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Invalid format rejected with 400"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 400, got $http_code"
    echo "Response: $body"
fi

# Test 5: Missing Credit Card Number
echo -e "\n${YELLOW}Test 5: Missing Credit Card Number${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/authorize" \
  -H "Content-Type: application/json" \
  -d '{}')
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" == "400" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Missing field rejected with 400"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 400, got $http_code"
    echo "Response: $body"
fi

# Test 6: Authorization Rate Test (should be ~90% success)
echo -e "\n${YELLOW}Test 6: Authorization Rate Test (100 requests)${NC}"
success_count=0
decline_count=0
error_count=0

for i in {1..100}; do
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/authorize" \
      -H "Content-Type: application/json" \
      -d '{"credit_card_number": "1234-5678-9012-3456"}')

    if [ "$http_code" == "200" ]; then
        ((success_count++))
    elif [ "$http_code" == "402" ]; then
        ((decline_count++))
    else
        ((error_count++))
    fi
done

success_rate=$((success_count))
decline_rate=$((decline_count))

echo "Results:"
echo "  Authorized (200): $success_count/100 (${success_rate}%)"
echo "  Declined (402):   $decline_count/100 (${decline_rate}%)"
echo "  Errors:           $error_count/100"

if [ $success_rate -ge 85 ] && [ $success_rate -le 95 ] && [ $error_count -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC} - Authorization rate is within expected range (85-95%)"
else
    echo -e "${YELLOW}⚠ WARNING${NC} - Authorization rate might be outside expected range"
    echo "Expected: ~90% authorized, ~10% declined"
fi

echo -e "\n========================================="
echo "Testing Complete"
echo "========================================="