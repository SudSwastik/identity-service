variable "environment" {
  description = "Deployment environment (dev, prod)"
  type        = string
}

variable "app_name" {
  description = "Application name used as a prefix for resource names"
  type        = string
  default     = "identity-service"
}

variable "ses_from_email" {
  description = "Verified SES email address used by Cognito to send emails"
  type        = string
}

variable "ses_arn" {
  description = "ARN of the verified SES identity (domain or email)"
  type        = string
}

variable "access_token_validity_minutes" {
  description = "Access token validity in minutes"
  type        = number
  default     = 60
}

variable "refresh_token_validity_days" {
  description = "Refresh token validity in days"
  type        = number
  default     = 30
}
