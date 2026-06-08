variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "domain" {
  description = "Domain to verify with SES and use for Cognito email sending"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID for RDS"
  type        = string
}

variable "subnet_ids" {
  description = "Subnet IDs for RDS subnet group (at least 2 AZs)"
  type        = list(string)
}

variable "app_security_group_id" {
  description = "Security group ID of the app container / ECS task"
  type        = string
}

variable "db_password" {
  description = "Master password for the RDS PostgreSQL instance"
  type        = string
  sensitive   = true
}
