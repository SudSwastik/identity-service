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
  #   key    = "identity-service/dev/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.aws_region
}

module "ses" {
  source = "../../modules/ses"

  domain      = var.domain
  environment = "dev"
}

module "cognito" {
  source = "../../modules/cognito"

  environment    = "dev"
  ses_from_email = "noreply@${var.domain}"
  ses_arn        = module.ses.domain_identity_arn
}

module "rds" {
  source = "../../modules/rds"

  environment               = "dev"
  vpc_id                    = var.vpc_id
  subnet_ids                = var.subnet_ids
  allowed_security_group_id = var.app_security_group_id
  db_password               = var.db_password
  instance_class            = "db.t3.micro"
  multi_az                  = false
}
