output "domain_identity_arn" {
  description = "ARN of the SES domain identity — pass to cognito module as ses_arn"
  value       = aws_ses_domain_identity.this.arn
}

output "verification_token" {
  description = "TXT record value for domain verification — add as _amazonses.<domain> TXT record"
  value       = aws_ses_domain_identity.this.verification_token
}

output "dkim_tokens" {
  description = "DKIM CNAME tokens — add as <token>._domainkey.<domain> CNAME records"
  value       = aws_ses_domain_dkim.this.dkim_tokens
}

output "smtp_username" {
  description = "SMTP username (IAM access key ID)"
  value       = aws_iam_access_key.ses_smtp.id
}

output "smtp_password" {
  description = "SMTP password (derived from IAM secret key)"
  value       = aws_iam_access_key.ses_smtp.ses_smtp_password_v4
  sensitive   = true
}
