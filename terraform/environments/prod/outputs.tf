output "cognito_user_pool_id" {
  value = module.cognito.user_pool_id
}

output "cognito_client_id" {
  value = module.cognito.client_id
}

output "cognito_client_secret" {
  value     = module.cognito.client_secret
  sensitive = true
}

output "cognito_issuer_uri" {
  value = module.cognito.issuer_uri
}

output "cognito_jwks_uri" {
  value = module.cognito.jwks_uri
}

output "rds_jdbc_url" {
  value = module.rds.jdbc_url
}

output "ses_smtp_username" {
  value = module.ses.smtp_username
}

output "ses_smtp_password" {
  value     = module.ses.smtp_password
  sensitive = true
}

output "ses_dns_verification_token" {
  description = "Add as TXT record: _amazonses.<domain>"
  value       = module.ses.verification_token
}

output "ses_dkim_tokens" {
  description = "Add as CNAME records: <token>._domainkey.<domain>"
  value       = module.ses.dkim_tokens
}
