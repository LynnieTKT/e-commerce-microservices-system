#!/bin/bash

# Build and Push CCA to Team's ECR
# Run from project root: ./scripts/build-cca.sh

set -e

# Team's AWS Configuration (from CSV)
AWS_ACCOUNT_ID="533267331581"
AWS_REGION="us-east-1"
ECR_REPO="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
SERVICE_NAME="credit-card-authorizer"
IMAGE_URI="${ECR_REPO}/${SERVICE_NAME}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}=== Building and Pushing CCA Service ===${NC}"
echo "ECR Repository: ${ECR_REPO}"
echo "Service: ${SERVICE_NAME}"
echo ""

# Navigate to CCA directory
cd services/credit-card-authorizer

# Step 1: Build JAR with Maven
echo -e "${YELLOW}Step 1: Building application with Maven...${NC}"
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}Maven build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Maven build successful${NC}"

# Step 2: Build Docker image
echo -e "\n${YELLOW}Step 2: Building Docker image...${NC}"
docker build -t ${SERVICE_NAME}:latest .

if [ $? -ne 0 ]; then
    echo -e "${RED}Docker build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Docker build successful${NC}"

# Step 3: Create ECR repository if it doesn't exist
echo -e "\n${YELLOW}Step 3: Checking ECR repository...${NC}"
aws ecr describe-repositories \
    --repository-names ${SERVICE_NAME} \
    --region ${AWS_REGION} 2>/dev/null || \
aws ecr create-repository \
    --repository-name ${SERVICE_NAME} \
    --region ${AWS_REGION}

# Step 4: Login to ECR
echo -e "\n${YELLOW}Step 4: Logging into ECR...${NC}"
aws ecr get-login-password --region ${AWS_REGION} | \
    docker login --username AWS --password-stdin ${ECR_REPO}

if [ $? -ne 0 ]; then
    echo -e "${RED}ECR login failed! Check your AWS credentials.${NC}"
    exit 1
fi
echo -e "${GREEN}✓ ECR login successful${NC}"

# Step 5: Tag image
echo -e "\n${YELLOW}Step 5: Tagging image...${NC}"
docker tag ${SERVICE_NAME}:latest ${IMAGE_URI}:latest
docker tag ${SERVICE_NAME}:latest ${IMAGE_URI}:v1.0

# Step 6: Push to ECR
echo -e "\n${YELLOW}Step 6: Pushing to ECR...${NC}"
docker push ${IMAGE_URI}:latest
docker push ${IMAGE_URI}:v1.0

if [ $? -ne 0 ]; then
    echo -e "${RED}Push to ECR failed!${NC}"
    exit 1
fi

echo -e "\n${GREEN}=== Build and Push Complete! ===${NC}"
echo -e "\nImage URIs:"
echo -e "  Latest: ${GREEN}${IMAGE_URI}:latest${NC}"
echo -e "  Tagged: ${GREEN}${IMAGE_URI}:v1.0${NC}"
echo -e "\nYou can now deploy EC2 instances using this image."

# Save image URI for later use
cd ../..
echo "${IMAGE_URI}:latest" > .cca-image-uri

echo -e "\n${YELLOW}Next step: ./scripts/deploy-cca.sh${NC}"