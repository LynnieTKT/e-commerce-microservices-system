#!/bin/bash

# Deploy CCA EC2 Instances
# Run from project root: ./scripts/deploy-cca.sh [plan|apply|destroy]

set -e

ACTION="${1:-plan}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================${NC}"
echo -e "${BLUE}CS6650 A3 - CCA Deployment${NC}"
echo -e "${BLUE}=========================================${NC}"
echo ""

# Check if terraform directory exists
if [ ! -d "terraform" ]; then
    echo -e "${RED}Error: terraform directory not found!${NC}"
    echo "Make sure you're in the project root directory"
    exit 1
fi

cd terraform

# Initialize Terraform if needed
if [ ! -d ".terraform" ]; then
    echo -e "${YELLOW}Initializing Terraform...${NC}"
    terraform init
    echo ""
fi

case $ACTION in
    plan)
        echo -e "${YELLOW}Creating Terraform plan...${NC}"
        terraform plan

        echo -e "\n${GREEN}Plan created successfully!${NC}"
        echo -e "${YELLOW}Review the plan above.${NC}"
        echo -e "To apply: ${GREEN}./scripts/deploy-cca.sh apply${NC}"
        ;;

    apply)
        echo -e "${YELLOW}Deploying CCA instances...${NC}"
        echo ""

        terraform apply -auto-approve

        if [ $? -eq 0 ]; then
            echo -e "\n${GREEN}=========================================${NC}"
            echo -e "${GREEN}Deployment Successful!${NC}"
            echo -e "${GREEN}=========================================${NC}"
            echo ""

            echo -e "${YELLOW}Instance Information:${NC}"
            terraform output cca_instance_ids
            echo ""
            terraform output cca_instance_public_ips
            echo ""

            echo -e "${YELLOW}SSH Commands:${NC}"
            terraform output -raw ssh_commands
            echo ""

            echo -e "\n${YELLOW}Important:${NC}"
            echo "1. Wait 2-3 minutes for Docker containers to start"
            echo "2. Check target health in AWS Console:"
            echo "   EC2 → Target Groups → cca-tg → Targets"
            echo "3. Get ALB DNS from Person 1 and test:"
            echo "   ./scripts/test-cca-alb.sh <ALB-DNS>"
            echo ""

            terraform output -raw next_steps
        else
            echo -e "${RED}Deployment failed!${NC}"
            exit 1
        fi
        ;;

    destroy)
        echo -e "${RED}WARNING: This will destroy all CCA instances!${NC}"
        read -p "Are you sure? Type 'yes' to confirm: " confirm

        if [ "$confirm" = "yes" ]; then
            echo -e "${YELLOW}Destroying CCA instances...${NC}"
            terraform destroy -auto-approve

            if [ $? -eq 0 ]; then
                echo -e "${GREEN}CCA instances destroyed successfully${NC}"
            else
                echo -e "${RED}Destroy failed!${NC}"
                exit 1
            fi
        else
            echo -e "${YELLOW}Destroy cancelled${NC}"
        fi
        ;;

    status)
        echo -e "${YELLOW}Current deployment status:${NC}"
        terraform show
        ;;

    output)
        echo -e "${YELLOW}Deployment outputs:${NC}"
        terraform output
        ;;

    *)
        echo -e "${RED}Invalid action: $ACTION${NC}"
        echo "Usage: ./scripts/deploy-cca.sh [plan|apply|destroy|status|output]"
        echo ""
        echo "Commands:"
        echo "  plan    - Show what will be created"
        echo "  apply   - Deploy the instances"
        echo "  destroy - Remove all instances"
        echo "  status  - Show current state"
        echo "  output  - Show deployment outputs"
        exit 1
        ;;
esac

cd ..