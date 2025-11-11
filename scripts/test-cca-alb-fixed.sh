#!/bin/bash

# Test script for CCA through ALB - macOS compatible
# Usage: ./test-cca-alb-fixed.sh [ALB_DNS]

set -e

# Get ALB DNS
if [ -z "$1" ]; then
    echo "Usage: ./test-cca-alb-fixed.sh <ALB_DNS>"
    echo ""
    echo "Example:"
    echo "  ./test-cca-alb-fixed.sh 6650-alb-2078178610.us-east-1.elb.amazonaws.com"
    exit 1
fi

ALB_DNS="$1"
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
http_code=$(curl -s -o /tmp/cca_test_response.txt -w "%{http_code}" "${BASE_URL}/credit-card-authorizer/health")
body=$(cat /tmp/cca_test_response.txt)

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Health check successful (HTTP $http_code)"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Health check failed (HTTP $http_code)"
    echo "Response: $body"
fi

# Test 2: Valid Credit Card
echo -e "\n${YELLOW}Test 2: Valid Credit Card Authorization${NC}"
http_code=$(curl -s -o /tmp/cca_test_response.txt -w "%{http_code}" \
  -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "1234-5678-9012-3456"}')
body=$(cat /tmp/cca_test_response.txt)

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
http_code=$(curl -s -o /tmp/cca_test_response.txt -w "%{http_code}" \
  -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
  -H "Content-Type: application/json" \
  -d '{"credit_card_number": "invalid"}')
body=$(cat /tmp/cca_test_response.txt)

if [ "$http_code" = "400" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Invalid format rejected (HTTP 400)"
    echo "Response: $body"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 400, got $http_code"
    echo "Response: $body"
fi

# Test 4: Missing Credit Card Number
echo -e "\n${YELLOW}Test 4: Missing Credit Card Number${NC}"
http_code=$(curl -s -o /tmp/cca_test_response.txt -w "%{http_code}" \
  -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
  -H "Content-Type: application/json" \
  -d '{}')

if [ "$http_code" = "400" ]; then
    echo -e "${GREEN}✓ PASS${NC} - Missing field rejected (HTTP 400)"
else
    echo -e "${RED}✗ FAIL${NC} - Expected 400, got $http_code"
fi

# Test 5: Authorization Rate Test (50 requests)
echo -e "\n${YELLOW}Test 5: Authorization Rate Test (50 requests)${NC}"
success=0
decline=0
error=0

echo -n "Progress: "
for i in {1..50}; do
    http_code=$(curl -s -o /dev/null -w "%{http_code}" \
      -X POST "${BASE_URL}/credit-card-authorizer/authorize" \
      -H "Content-Type: application/json" \
      -d '{"credit_card_number": "1234-5678-9012-3456"}')

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
        echo -e "${RED}✗ FAIL${NC} - Errors detected"
    else
        echo -e "${YELLOW}⚠ WARNING${NC} - Rate outside expected range (target: ~90%)"
    fi
fi

# Test 6: Load Distribution
echo -e "\n${YELLOW}Test 6: Load Distribution (10 requests)${NC}"
echo "Sending 10 requests to verify load balancing..."

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
echo "To check target health:"
echo "  aws elbv2 describe-target-health \\"
echo "    --target-group-arn arn:aws:elasticloadbalancing:us-east-1:533267331581:targetgroup/cca-tg/b7db4eb8e6f10afe \\"
echo "    --region us-east-1"
echo ""
echo "To SSH into instances:"
echo "  ssh -i ~/.ssh/Team.pem ec2-user@<instance-ip>"

# Cleanup
rm -f /tmp/cca_test_response.txt