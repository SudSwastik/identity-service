variable "environment" {
  description = "Deployment environment (dev, prod)"
  type        = string
}

variable "app_name" {
  description = "Application name used as a prefix for resource names"
  type        = string
  default     = "identity-service"
}

variable "vpc_id" {
  description = "VPC ID where the RDS instance will be deployed"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs for the RDS subnet group (at least 2 AZs)"
  type        = list(string)
}

variable "allowed_security_group_id" {
  description = "Security group ID of the app (allowed to reach RDS on port 5432)"
  type        = string
}

variable "db_name" {
  description = "Name of the PostgreSQL database"
  type        = string
  default     = "identity_service"
}

variable "db_username" {
  description = "Master username for the RDS instance"
  type        = string
  default     = "identity_user"
}

variable "db_password" {
  description = "Master password for the RDS instance"
  type        = string
  sensitive   = true
}

variable "instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "allocated_storage_gb" {
  description = "Allocated storage in gigabytes"
  type        = number
  default     = 20
}

variable "multi_az" {
  description = "Enable Multi-AZ for high availability"
  type        = bool
  default     = false
}
