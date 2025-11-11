#!/bin/bash

# Quick check to find the right VPC and resources
# This will find everything you need in one go

echo "🔍 Finding correct AWS resources..."
echo ""

# Find the CCA target group first
echo "1. Looking for CCA Target Group..."
TG_INFO=$(aws elbv2 describe-target-groups --region us-east-1 \
  --query 'TargetGroups[?TargetGroupName==`cca-tg`] | [0].[TargetGroupArn, VpcId, Port]' \
  --output text)

if [ -z "$TG_INFO" ]; then
    echo "❌ cca-tg target group not found!"
    echo "Available target groups:"
    aws elbv2 describe-target-groups --region us-east-1 \
      --query 'TargetGroups[*].[TargetGroupName, Port, VpcId]' \
      --output table
    exit 1
fi

TG_ARN=$(echo $TG_INFO | awk '{print $1}')
VPC_ID=$(echo $TG_INFO | awk '{print $2}')
TG_PORT=$(echo $TG_INFO | awk '{print $3}')

echo "✓ Found cca-tg in VPC: $VPC_ID"
echo ""

# Find subnets in that VPC
echo "2. Finding subnets in VPC $VPC_ID..."
SUBNETS=$(aws ec2 describe-subnets --region us-east-1 \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'Subnets[*].[SubnetId, AvailabilityZone]' \
  --output text | head -2)

SUBNET_1=$(echo "$SUBNETS" | head -1 | awk '{print $1}')
SUBNET_2=$(echo "$SUBNETS" | tail -1 | awk '{print $1}')

echo "✓ Found subnets: $SUBNET_1, $SUBNET_2"
echo ""

# Find security groups in that VPC
echo "3. Finding security groups in VPC $VPC_ID..."
echo ""
echo "Available security groups:"
aws ec2 describe-security-groups --region us-east-1 \
  --filters "Name=vpc-id,Values=$VPC_ID" \
  --query 'SecurityGroups[*].[GroupId, GroupName, Description]' \
  --output table

echo ""
echo "Which security group should EC2 instances use?"
echo "Look for one that allows inbound on ports 8080-8082"
read -p "Enter Security Group ID: " SG_ID
echo ""

# Find ALB
echo "4. Finding Load Balancer..."
ALB_DNS=$(aws elbv2 describe-load-balancers --region us-east-1 \
  --query 'LoadBalancers[?VpcId==`'$VPC_ID'`] | [0].DNSName' \
  --output text)

echo "✓ ALB DNS: $ALB_DNS"
echo ""

# Summary
echo "================================================"
echo "✅ CORRECT VALUES FOR YOUR ENVIRONMENT"
echo "================================================"
echo ""
echo "Copy these to terraform/variables.tf:"
echo ""
echo "vpc_id            = \"$VPC_ID\""
echo "subnet_ids        = [\"$SUBNET_1\", \"$SUBNET_2\"]"
echo "security_group_id = \"$SG_ID\""
echo "target_group_arn  = \"$TG_ARN\""
echo "key_name          = \"Team\""
echo ""
echo "ALB DNS for testing:"
echo "ALB_DNS = \"$ALB_DNS\""
echo ""
echo "================================================"

# Save to file
cat > terraform/correct-values.txt <<EOF
# Correct values discovered on $(date)

variable "vpc_id" {
  default = "$VPC_ID"
}

variable "subnet_ids" {
  default = ["$SUBNET_1", "$SUBNET_2"]
}

variable "security_group_id" {
  default = "$SG_ID"
}

variable "target_group_arn" {
  default = "$TG_ARN"
}

variable "key_name" {
  default = "Team"
}
EOF

echo "✓ Saved to terraform/correct-values.txt"