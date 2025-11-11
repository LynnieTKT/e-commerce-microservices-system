#!/bin/bash

# Test CCA through existing ALB
# Usage: ./scripts/test-cca-alb.sh [ALB_DNS]

set -e

# Get ALB DNS
if [ -z "$1" ]; then
    echo "Usage: ./scripts/test-cca-alb.sh <ALB_DNS>"
    echo ""
    echo "Example:"
    echo "  ./scripts/test-cca-alb.sh 6650-alb-1234567890.us-east-1.elb.amazonaws.com"
    echo ""
    echo "Ask Person 1 for the ALB DNS name, or find it in AWS Console:"
    echo "  EC2 → Load Balancers → 6650-alb → DNS name"
    exit 1
fi

ALB_DNS="6650-alb-2078178610.us-east-1.elb.amazonaws.com"
BASE_URL="http://${ALB_DNS}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}Testing CCA through ALB${NC}"
echo -e "${BLUE}=========================================${NC}"
echo -e "ALB DNS: ${YELLOW}${ALB_DNS}${NC}"
echo -e "Base URL: ${YELLOW}${BASE_URL}${NC}"
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: CCA Health Check${NC}"
response=$(curl -s -w "\n%{http_code}" "${BASE_URL}/credit-card-authorizer/health" || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Health check successful"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Health check failed (HTTP $http_code)"
    if [ "$http_code" = "000" ]; then
        echo "Cannot reach ALB. Check:"
        echo "  1. ALB DNS is correct"
        echo "  2. Security groups allow traffic"
        echo "  3. Instances are registered in target group"
    else
        echo "Response: $body"
    fi
    echo -e "\n${YELLOW}Note: Instances may still be initializing. Wait 2-3 minutes.${NC}"
fi

# Test 2: Valid Credit Card
echo -e "\n${YELLOW}Test 2: Valid Credit Card Authorization${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234-5678-9012-3456"}' || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Authorization APPROVED (HTTP 200)"
elif [ "$http_code" = "402" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Authorization DECLINED (HTTP 402)"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Unexpected response (HTTP $http_code)"
    echo "Response: $body"
fi

# Test 3: Invalid Format
echo -e "\n${YELLOW}Test 3: Invalid Credit Card Format${NC}"
response=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "invalid"}' || echo -e "\n000")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n-1)

if [ "$http_code" = "400" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Invalid format rejected (HTTP 400)"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 400, got $http_code"
fi

# Test 4: Authorization Rate
echo -e "\n${YELLOW}Test 4: Authorization Rate Test (50 requests)${NC}"
success=0
decline=0
error=0

for i in {1..50}; do
    http_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
      -H "Content-Type: application/json" \
      -d '{"credit_card_number": "1234-5678-9012-3456"}' || echo "000")

    if [ "$http_code" = "200" ]; then
        ((success++))
    elif [ "$http_code" = "402" ]; then
        ((decline++))
    else
        ((error++))
    fi

    # Show progress
    if [ $((i % 10)) -eq 0 ]; then
        echo -n "."
    fi
done

echo ""

success_rate=$((success * 100 / 50))
decline_rate=$((decline * 100 / 50))

echo "Results:"
echo "  Authorized (200): $success/50 (${success_rate}%)"
echo "  Declined (402):   $decline/50 (${decline_rate}%)"
echo "  Errors:           $error/50"

if [ $success_rate -ge 80 ] && [ $success_rate -le 95 ] && [ $error -eq 0 ]; then
    echo -e "${GREEN}✓ PASS${NC} - Authorization rate is within expected range (80-95%)"
else
    if [ $error -gt 0 ]; then
        echo -e "${RED}✗ FAIL${NC} - Errors detected. Check instance health."
    else
        echo -e "${YELLOW}⚠ WARNING${NC} - Rate outside expected range (target: ~90%)"
    fi
fi

# Test 5: Load Distribution
echo -e "\n${YELLOW}Test 5: Load Distribution (checking both instances)${NC}"
echo "Making 10 requests to verify load balancing..."

for i in {1..10}; do
    curl -s "${BASE_URL}/credit-card-authorizer/health" > /dev/null
done

echo -e "${GREEN}✓${NC} Requests sent. Check AWS Console for distribution:"
echo "  EC2 → Target Groups → cca-tg → Monitoring"

echo -e "\n${BLUE}=========================================${NC}"
echo -e "${BLUE}Testing Complete${NC}"
echo -e "${BLUE}=========================================${NC}"

# Summary
echo -e "\n${YELLOW}Summary:${NC}"
echo "CCA Endpoint: ${BASE_URL}/credit-card-authorizer/authorize"
echo ""
echo "Check AWS Console for detailed metrics:"
echo "  1. EC2 → Target Groups → cca-tg → Targets (health status)"
echo "  2. EC2 → Target Groups → cca-tg → Monitoring (traffic)"
echo "  3. CloudWatch → Logs (if configured)"
echo ""
echo "To SSH into instances:"
cd terraform && terraform output ssh_commands 2>/dev/null || echo "  Run: cd terraform && terraform output ssh_commands"