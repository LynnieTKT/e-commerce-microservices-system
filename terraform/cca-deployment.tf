# Terraform configuration for CCA EC2 instances
# MODIFIED FOR AWS ACADEMY - No IAM role required!

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project    = var.project_name
      Service    = "credit-card-authorizer"
      ManagedBy  = "Terraform"
      Owner      = "Person3"
      Assignment = "CS6650-A3"
    }
  }
}

# Data source for latest Amazon Linux 2 AMI
data "aws_ami" "amazon_linux_2" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# User data script for CCA instances
# Uses AWS CLI with instance credentials (from voclabs role)
locals {
  cca_user_data = <<-EOF
    #!/bin/bash
    set -e

    # Log everything
    exec > >(tee /var/log/user-data.log)
    exec 2>&1

    echo "Starting CCA deployment at $(date)"

    # Update system
    yum update -y

    # Install Docker
    amazon-linux-extras install docker -y
    systemctl start docker
    systemctl enable docker
    usermod -a -G docker ec2-user

    # AWS CLI is pre-installed on Amazon Linux 2
    # The instance inherits credentials from the voclabs role

    # Login to ECR using inherited credentials
    echo "Logging into ECR..."
    aws ecr get-login-password --region ${var.aws_region} | \
        docker login --username AWS --password-stdin ${var.aws_account_id}.dkr.ecr.${var.aws_region}.amazonaws.com

    if [ $? -ne 0 ]; then
        echo "ECR login failed, retrying in 10 seconds..."
        sleep 10
        aws ecr get-login-password --region ${var.aws_region} | \
            docker login --username AWS --password-stdin ${var.aws_account_id}.dkr.ecr.${var.aws_region}.amazonaws.com
    fi

    # Pull CCA image
    echo "Pulling CCA image..."
    docker pull ${var.cca_docker_image}

    if [ $? -ne 0 ]; then
        echo "Failed to pull image, retrying..."
        sleep 10
        docker pull ${var.cca_docker_image}
    fi

    # Run CCA container
    echo "Starting CCA container..."
    docker run -d \
      -p 8082:8082 \
      --name cca-service \
      --restart always \
      --log-driver=json-file \
      --log-opt max-size=10m \
      --log-opt max-file=3 \
      ${var.cca_docker_image}

    # Wait for container to start
    sleep 15

    # Verify container is running
    if docker ps | grep -q cca-service; then
      echo "CCA service started successfully"

      # Test health endpoint
      for i in {1..5}; do
        if curl -f http://localhost:8082/credit-card-authorizer/health; then
          echo "Health check passed"
          break
        else
          echo "Health check failed, retrying in 10s..."
          sleep 10
        fi
      done
    else
      echo "CCA service failed to start"
      docker logs cca-service
      exit 1
    fi

    echo "CCA deployment completed at $(date)"
  EOF
}

# EC2 Instances for CCA
# NO IAM instance profile - uses inherited voclabs credentials
resource "aws_instance" "cca" {
  count = var.cca_instance_count

  ami                    = data.aws_ami.amazon_linux_2.id
  instance_type          = var.instance_type
  key_name               = var.key_name
  vpc_security_group_ids = [var.security_group_id]
  subnet_id              = var.subnet_ids[count.index % length(var.subnet_ids)]


  # IMPORTANT: Assign public IP for SSH access and debugging
  associate_public_ip_address = true

  user_data = local.cca_user_data

  # Enable detailed monitoring
  monitoring = true

  # Storage
  root_block_device {
    volume_size = 20
    volume_type = "gp3"
  }

  tags = {
    Name    = "${var.project_name}-cca-${count.index + 1}"
    Service = "credit-card-authorizer"
    Index   = count.index + 1
  }
}

# Register CCA instances with existing target group
resource "aws_lb_target_group_attachment" "cca" {
  count = var.cca_instance_count

  target_group_arn = var.target_group_arn
  target_id        = aws_instance.cca[count.index].id
  port             = 8082
}