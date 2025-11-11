# Variables for CCA EC2 Deployment
# Using existing infrastructure created by Person 1

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "aws_account_id" {
  description = "AWS Account ID"
  type        = string
  default     = "533267331581"
}

# Existing Infrastructure (from Person 1)
variable "vpc_id" {
  description = "Existing VPC ID"
  type        = string
  default     = "vpc-004ed97e70b276c16"
}


variable "subnet_ids" {
  description = "Existing subnet IDs"
  type        = list(string)
  default     = ["subnet-0e6981c6a26419f79", "subnet-0c1b3cdad848ad02d"]
}

variable "security_group_id" {
  description = "Existing security group for EC2 instances"
  type        = string
  default     = "sg-05699ac24060209d8"
}

variable "target_group_arn" {
  description = "Existing CCA target group ARN"
  type        = string
  default     = "arn:aws:elasticloadbalancing:us-east-1:533267331581:targetgroup/cca-tg/b7db4eb8e6f10afe"
}

variable "key_name" {
  description = "EC2 key pair name"
  type        = string
  default     = "Team"
}

# CCA Configuration
variable "cca_instance_count" {
  description = "Number of CCA instances to create"
  type        = number
  default     = 2
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t2.micro"
}

variable "cca_docker_image" {
  description = "CCA Docker image URI in ECR"
  type        = string
  default     = "533267331581.dkr.ecr.us-east-1.amazonaws.com/credit-card-authorizer:latest"
}

variable "project_name" {
  description = "Project name for tagging"
  type        = string
  default     = "cs6650-group13"
}