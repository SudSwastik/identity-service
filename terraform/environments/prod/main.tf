terraform {
  required_version = ">= 1.6"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # Uncomment to store state in S3
  # backend "s3" {
  #   bucket = "your-terraform-state-bucket"
  #   key    = "identity-service/prod/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.aws_region
}

module "ses" {
  source = "../../modules/ses"

  domain      = var.domain
  environment = "prod"
}

module "cognito" {
  source = "../../modules/cognito"

  environment    = "prod"
  ses_from_email = "noreply@${var.domain}"
  ses_arn        = module.ses.domain_identity_arn

  access_token_validity_minutes = 60
  refresh_token_validity_days   = 30
}

module "rds" {
  source = "../../modules/rds"

  environment               = "prod"
  vpc_id                    = var.vpc_id
  subnet_ids                = var.subnet_ids
  allowed_security_group_id = var.app_security_group_id
  db_password               = var.db_password
  instance_class            = var.db_instance_class
  allocated_storage_gb      = var.db_allocated_storage_gb
  multi_az                  = true
}
