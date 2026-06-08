variable "domain" {
  description = "Domain to verify with SES (e.g. example.com)"
  type        = string
}

variable "environment" {
  description = "Deployment environment (dev, prod)"
  type        = string
}
