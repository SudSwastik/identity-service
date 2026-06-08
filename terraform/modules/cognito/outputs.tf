output "user_pool_id" {
  description = "Cognito User Pool ID"
  value       = aws_cognito_user_pool.this.id
}

output "user_pool_arn" {
  description = "Cognito User Pool ARN"
  value       = aws_cognito_user_pool.this.arn
}

output "client_id" {
  description = "Cognito App Client ID"
  value       = aws_cognito_user_pool_client.this.id
}

output "client_secret" {
  description = "Cognito App Client Secret"
  value       = aws_cognito_user_pool_client.this.client_secret
  sensitive   = true
}

output "issuer_uri" {
  description = "JWT issuer URI for Spring Security oauth2ResourceServer config"
  value       = "https://cognito-idp.${data.aws_region.current.name}.amazonaws.com/${aws_cognito_user_pool.this.id}"
}

output "jwks_uri" {
  description = "JWKS endpoint for JWT signature verification"
  value       = "https://cognito-idp.${data.aws_region.current.name}.amazonaws.com/${aws_cognito_user_pool.this.id}/.well-known/jwks.json"
}

data "aws_region" "current" {}
