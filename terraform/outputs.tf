# Outputs for CCA deployment

# CCA Instance Information
output "cca_instance_ids" {
  description = "IDs of CCA EC2 instances"
  value       = aws_instance.cca[*].id
}

output "cca_instance_public_ips" {
  description = "Public IP addresses of CCA instances"
  value       = aws_instance.cca[*].public_ip
}

output "cca_instance_private_ips" {
  description = "Private IP addresses of CCA instances"
  value       = aws_instance.cca[*].private_ip
}

# SSH Connection Info
output "ssh_commands" {
  description = "SSH commands to connect to CCA instances"
  value = [
    for i, instance in aws_instance.cca :
    "ssh -i ~/.ssh/${var.key_name}.pem ec2-user@${instance.public_ip}"
  ]
}

# Existing Infrastructure Info
output "target_group_arn" {
  description = "CCA Target Group ARN (existing)"
  value       = var.target_group_arn
}

output "security_group_id" {
  description = "Security Group ID (existing)"
  value       = var.security_group_id
}

output "vpc_id" {
  description = "VPC ID (existing)"
  value       = var.vpc_id
}

# Deployment Summary
output "deployment_summary" {
  description = "Summary of CCA deployment"
  value = {
    instance_count = var.cca_instance_count
    instance_type  = var.instance_type
    docker_image   = var.cca_docker_image
    region         = var.aws_region
  }
}

# Next Steps
output "next_steps" {
  description = "What to do next"
  value = <<-EOT
    ✓ CCA instances deployed successfully!

    Next steps:
    1. Wait 2-3 minutes for Docker containers to start
    2. Check target health in AWS Console:
       EC2 → Target Groups → cca-tg → Targets tab
    3. Test through ALB: ./scripts/test-cca-alb.sh
    4. SSH to instances if needed (see ssh_commands output)

    To get ALB DNS from Person 1's deployment:
    - Ask Person 1 for the ALB DNS name
    - Or check AWS Console: EC2 → Load Balancers
  EOT
}