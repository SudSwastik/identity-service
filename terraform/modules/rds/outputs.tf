output "endpoint" {
  description = "RDS instance endpoint (host:port)"
  value       = aws_db_instance.this.endpoint
}

output "db_name" {
  description = "Database name"
  value       = aws_db_instance.this.db_name
}

output "jdbc_url" {
  description = "JDBC connection URL for Spring Boot datasource config"
  value       = "jdbc:postgresql://${aws_db_instance.this.endpoint}/${aws_db_instance.this.db_name}"
}

output "security_group_id" {
  description = "RDS security group ID"
  value       = aws_security_group.rds.id
}
