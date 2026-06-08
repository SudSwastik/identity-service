resource "aws_ses_domain_identity" "this" {
  domain = var.domain
}

resource "aws_ses_domain_dkim" "this" {
  domain = aws_ses_domain_identity.this.domain
}

# SMTP credentials for the application (SES v2 does not support SMTP directly;
# SMTP credentials are IAM user access keys encoded via the SES SDK formula).
resource "aws_iam_user" "ses_smtp" {
  name = "identity-service-ses-smtp-${var.environment}"

  tags = {
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

resource "aws_iam_user_policy" "ses_send" {
  name = "ses-send-email"
  user = aws_iam_user.ses_smtp.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect   = "Allow"
      Action   = ["ses:SendEmail", "ses:SendRawEmail"]
      Resource = aws_ses_domain_identity.this.arn
    }]
  })
}

resource "aws_iam_access_key" "ses_smtp" {
  user = aws_iam_user.ses_smtp.name
}
