locals {
  prefix = "${var.app_name}-${var.environment}"
}

resource "aws_cognito_user_pool" "this" {
  name = "${local.prefix}-user-pool"

  # Username is the email address
  username_attributes      = ["email"]
  auto_verified_attributes = ["email"]

  # Password policy
  password_policy {
    minimum_length                   = 8
    require_lowercase                = true
    require_uppercase                = true
    require_numbers                  = true
    require_symbols                  = true
    temporary_password_validity_days = 7
  }

  # MFA — optional (users can opt in via the app)
  mfa_configuration = "OPTIONAL"
  software_token_mfa_configuration {
    enabled = true
  }

  # Email via SES
  email_configuration {
    email_sending_account = "DEVELOPER"
    from_email_address    = var.ses_from_email
    source_arn            = var.ses_arn
  }

  # Account recovery via email only
  account_recovery_setting {
    recovery_mechanism {
      name     = "verified_email"
      priority = 1
    }
  }

  # Standard attributes
  schema {
    name                     = "email"
    attribute_data_type      = "String"
    required                 = true
    mutable                  = true
    string_attribute_constraints {
      min_length = 5
      max_length = 254
    }
  }

  schema {
    name                     = "name"
    attribute_data_type      = "String"
    required                 = false
    mutable                  = true
    string_attribute_constraints {
      min_length = 1
      max_length = 255
    }
  }

  tags = {
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}

resource "aws_cognito_user_pool_client" "this" {
  name         = "${local.prefix}-app-client"
  user_pool_id = aws_cognito_user_pool.this.id

  # Server-side auth flows (app uses AdminInitiateAuth)
  explicit_auth_flows = [
    "ALLOW_ADMIN_USER_PASSWORD_AUTH",
    "ALLOW_REFRESH_TOKEN_AUTH",
    "ALLOW_USER_SRP_AUTH",
  ]

  # Token validity
  access_token_validity  = var.access_token_validity_minutes
  refresh_token_validity = var.refresh_token_validity_days
  id_token_validity      = var.access_token_validity_minutes

  token_validity_units {
    access_token  = "minutes"
    refresh_token = "days"
    id_token      = "minutes"
  }

  # Prevent client secret from being exposed (server-side only)
  generate_secret = true

  prevent_user_existence_errors = "ENABLED"
}

resource "aws_cognito_user_pool_domain" "this" {
  domain       = "${local.prefix}-auth"
  user_pool_id = aws_cognito_user_pool.this.id
}

# Seed groups (mirrored in PostgreSQL as roles)
resource "aws_cognito_user_group" "admin" {
  name         = "ADMIN"
  user_pool_id = aws_cognito_user_pool.this.id
  description  = "Administrator group"
  precedence   = 1
}

resource "aws_cognito_user_group" "user" {
  name         = "USER"
  user_pool_id = aws_cognito_user_pool.this.id
  description  = "Standard user group"
  precedence   = 2
}

resource "aws_cognito_user_group" "readonly" {
  name         = "READONLY"
  user_pool_id = aws_cognito_user_pool.this.id
  description  = "Read-only access group"
  precedence   = 3
}
