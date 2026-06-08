# Identity Service

A production-grade identity and access management service built with Spring Boot, AWS Cognito, and PostgreSQL.

## Architecture

Authentication is fully delegated to **AWS Cognito** (registration, login, token issuance, MFA). All authorization — roles, permissions, RBAC — lives exclusively in **PostgreSQL**, linked to Cognito users via the `sub` claim (UUID). Cognito groups are intentionally ignored; PostgreSQL is the single source of truth for access control. Spring Security validates JWTs automatically using Cognito's JWKS endpoint — no manual key management.

```
┌─────────────────────────────────────────────────────────────────────┐
│                            CLIENT                                   │
│                  (Browser / Mobile / API Consumer)                  │
└────────────────────────────┬────────────────────────────────────────┘
                             │
              Authorization: Bearer <Cognito JWT>
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       IDENTITY SERVICE                              │
│                        (Spring Boot 3.3)                            │
│                                                                     │
│  ┌─────────────────┐    ┌──────────────────────────────────────┐    │
│  │  SecurityConfig │    │         JwtAuthenticationConverter   │    │
│  │                 │    │                                      │    │
│  │  - Stateless    │───▶│  1. Extract sub claim                │    │
│  │  - JWKS cached  │    │  2. Query PostgreSQL user_roles      │    │
│  │  - Public:      │    │  3. Merge into GrantedAuthority set  │    │
│  │    /api/auth/** │    └──────────────────────────────────────┘    │
│  └─────────────────┘                                                │
│                                                                     │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────────────┐   │
│  │ AuthController│  │ MfaController │  │ RolesController        │   │
│  │               │  │               │  │ UsersController        │   │
│  │ /api/auth/**  │  │ /api/mfa/**   │  │ /api/roles/**          │   │
│  │               │  │               │  │ /api/users/**          │   │
│  └───────┬───────┘  └───────┬───────┘  └───────────┬────────────┘   │
│          │                  │                       │               │
│          ▼                  ▼                       ▼               │
│  ┌───────────────┐  ┌───────────────┐  ┌────────────────────────┐   │
│  │  AuthService  │  │  MfaService   │  │ RoleService            │   │
│  │               │  │               │  │ UserRoleService        │   │
│  └───────┬───────┘  └───────┬───────┘  └───────────┬────────────┘   │
│          │                  │                      │               │
└──────────┼──────────────────┼──────────────────────┼──────────────┘
           │                  │                      │
           ▼                  ▼                      ▼
┌─────────────────────┐              ┌──────────────────────────────┐
│    AWS Cognito      │             │        PostgreSQL             │
│                     │              │                               │
│  - User Pool        │              │  roles                        │
│  - Token issuance   │              │  permissions                  │
│  - MFA (TOTP)       │              │  role_permissions             │
│  - Password reset   │              │  user_roles (keyed by sub)    │
│  - Email verify     │              │                               │
└─────────────────────┘              └──────────────────────────────┘
           │
           ▼
┌─────────────────────┐
│    Email (SMTP)     │
│                     │
│  Local: Mailhog     │
│  Dev/Prod: AWS SES  │
└─────────────────────┘
```

## Request Auth Flow

```
Client Request (Bearer token)
         │
         ▼
Spring Security ──▶ Fetch/Cache Cognito JWKS ──▶ Validate JWT signature + expiry
         │
         ▼
JwtAuthenticationConverter (PostgreSQL-backed)
  ├── Extract sub claim (Cognito user UUID)
  └── UserRoleService.getAuthoritiesForUser(sub)
        ├── ROLE_<name> for each assigned role
        └── <resource>:<action> for each permission in those roles
      (cognito:groups intentionally ignored — PostgreSQL is authoritative)
         │
         ▼
Merged GrantedAuthority set on SecurityContextHolder
         │
         ▼
Controller Method (@PreAuthorize / hasAuthority checks)
```

## MFA Setup Flow

```
POST /api/mfa/setup  (Bearer required)
  └── Cognito AssociateSoftwareToken
        └── Returns { secretCode, qrCodeUri }
              qrCodeUri = otpauth://totp/<appName>:<username>?secret=...
              (render as QR code — compatible with Google Authenticator / Authy)

User scans QR code and enters the 6-digit TOTP code

POST /api/mfa/verify-setup  { userCode: "123456" }
  └── Cognito VerifySoftwareToken (validates the code)
        └── AdminSetUserMFAPreference (enables TOTP as preferred MFA)
              └── MFA active — login flow now returns a SOFTWARE_TOKEN_MFA challenge
```

## Database Schema

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PostgreSQL Schema                            │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────────────┐       ┌──────────────────────────┐
│          roles            │       │       permissions         │
├──────────────────────────┤       ├──────────────────────────┤
│ id          BIGSERIAL     │       │ id          BIGSERIAL     │
│ version     BIGINT        │       │ version     BIGINT        │
│ name        VARCHAR(100)  │       │ resource    VARCHAR(100)  │
│ description VARCHAR(255)  │       │ action      VARCHAR(50)   │
│ created_at  TSTZ          │       │ created_at  TSTZ          │
│ created_by  VARCHAR(255)  │       │ created_by  VARCHAR(255)  │
│ modified_at TSTZ          │       │ modified_at TSTZ          │
│ modified_by VARCHAR(255)  │       │ modified_by VARCHAR(255)  │
└────────────┬──────────────┘       └────────────┬─────────────┘
             │                                   │
             └──────────────┬────────────────────┘
                            │
                            ▼
              ┌──────────────────────────────┐
              │       role_permissions        │
              ├──────────────────────────────┤
              │ role_id       BIGINT ─────────┼──▶ roles.id
              │ permission_id BIGINT ─────────┼──▶ permissions.id
              │ assigned_at   TIMESTAMPTZ     │
              │ assigned_by   VARCHAR(255)    │
              │ PRIMARY KEY (role_id,         │
              │              permission_id)   │
              └──────────────────────────────┘

┌───────────────────────────────────┐
│             user_roles             │
├───────────────────────────────────┤
│ cognito_sub VARCHAR(255) NOT NULL  │  ◀── JWT sub claim (Cognito UUID)
│ role_id     BIGINT ───────────────┼──▶ roles.id
│ assigned_at TIMESTAMPTZ           │
│ assigned_by VARCHAR(255)          │  ◀── sub of admin who assigned
│ PRIMARY KEY (cognito_sub, role_id) │
└───────────────────────────────────┘

┌──────────────────────────────────────┐
│              audit_logs               │
├──────────────────────────────────────┤
│ id          UUID (gen_random_uuid())  │
│ entity_type VARCHAR(100)             │  e.g. "Role", "Permission"
│ entity_id   VARCHAR(255)             │  stringified PK
│ action      VARCHAR(50)              │  CREATE / UPDATE / DELETE / ASSIGN
│ actor_sub   VARCHAR(255)             │  Cognito sub of who did it
│ old_value   JSONB                    │  state before change
│ new_value   JSONB                    │  state after change
│ created_at  TIMESTAMPTZ              │
└──────────────────────────────────────┘

INDEXES:
  idx_user_roles_cognito_sub  ON user_roles  (cognito_sub)
  idx_audit_logs_entity       ON audit_logs  (entity_type, entity_id)
  idx_audit_logs_actor        ON audit_logs  (actor_sub)
  idx_audit_logs_created_at   ON audit_logs  (created_at DESC)
```

### Permission Identity

Permissions have no `name` column. Identity is `(resource, action)` with a `UNIQUE` constraint — e.g. `resource=users, action=read`. The application generates the display label as `resource:action` via `Permission.toPermissionString()`.

### Audit Fields

`roles` and `permissions` extend the `Auditable` base class. Fields are populated automatically by Spring Data JPA auditing; the auditor is the JWT `sub` of the authenticated user, or `"system"` for unauthenticated operations (Flyway seed, scheduled jobs).

| Column | Type | Set on | Description |
|--------|------|--------|-------------|
| `created_at` | TIMESTAMPTZ | INSERT | Timestamp of creation |
| `created_by` | VARCHAR | INSERT | Cognito `sub` of creator |
| `modified_at` | TIMESTAMPTZ | INSERT + UPDATE | Timestamp of last change |
| `modified_by` | VARCHAR | INSERT + UPDATE | Cognito `sub` of last modifier |
| `version` | BIGINT | Every UPDATE | Optimistic lock counter — prevents silent lost updates |

`role_permissions` and `user_roles` use `assigned_at` / `assigned_by` — these records are never updated, only created or deleted.

### Audit Log

`audit_logs` is append-only. It captures the full before/after state as JSONB, the entity type and ID, and the actor's Cognito `sub`. It answers questions like "who removed ADMIN from user X?" or "what did permission Y look like before it was changed?" that the inline audit columns cannot.

### Seed Data

| Role | Permissions |
|------|------------|
| ADMIN | users:read, users:write, users:delete, roles:read, roles:write, roles:delete, permissions:read, permissions:write |
| USER | users:read |
| READONLY | users:read, roles:read, permissions:read |

## API Reference

### Auth — `/api/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | Public | Create Cognito user |
| POST | `/login` | Public | Authenticate, returns JWT tokens |
| POST | `/refresh` | Public | Refresh access token |
| POST | `/logout` | Bearer | Revoke tokens (global sign-out) |
| POST | `/forgot-password` | Public | Trigger reset email |
| POST | `/reset-password` | Public | Confirm new password with code |
| POST | `/verify-email` | Public | Confirm email with Cognito code |
| POST | `/resend-verification` | Public | Resend verification code |
| POST | `/mfa-challenge` | Public | Complete MFA challenge after login (returns tokens) |

### MFA — `/api/mfa`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/setup` | Bearer | Enable TOTP — returns secret + QR URI |
| POST | `/verify-setup` | Bearer | Confirm TOTP code to activate MFA |
| DELETE | `/disable` | Bearer | Disable MFA |
| GET | `/status` | Bearer | Check if MFA is enabled |

### Roles — `/api/roles` (Admin only)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/` | ADMIN | List all roles |
| POST | `/` | ADMIN | Create role — returns `201 Created` |
| PUT | `/{id}` | ADMIN | Update role name/description |
| DELETE | `/{id}` | ADMIN | Delete role — returns `204 No Content` |
| POST | `/{id}/permissions` | ADMIN | Assign permissions to role (idempotent) |
| DELETE | `/{id}/permissions/{permissionId}` | ADMIN | Revoke a single permission from role |
| POST | `/assign` | ADMIN | Assign role to user (by sub) |
| DELETE | `/revoke` | ADMIN | Revoke role from user |

### Users — `/api/users`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/me` | Bearer | Current user profile + roles (Cognito + PostgreSQL) |
| PUT | `/me` | Bearer | Update Cognito `name` attribute |
| GET | `/{sub}` | ADMIN | Get any user by Cognito sub |
| POST | `/{sub}/disable` | ADMIN | Disable user (Cognito AdminDisableUser — reversible) |
| POST | `/{sub}/enable` | ADMIN | Re-enable a disabled user |

### Error Responses

All errors use [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457) format (`application/problem+json`):

| Status | Trigger |
|--------|---------|
| `400` | `AuthException` — Cognito rejected the request |
| `404` | `ResourceNotFoundException` — role, permission, or user not found |
| `409` | `ConflictException` or optimistic lock failure |
| `422` | Validation failure (`@Valid` / `@NotBlank` / `@Pattern` etc.) |

## Local Development

### Prerequisites

- Java 21
- Docker + Docker Compose
- Maven 3.9+

### Start

```bash
# Start Postgres + Mailhog
docker-compose -f docker/docker-compose.local.yml up postgres mailhog -d

# Run with local profile (Flyway migrations run automatically)
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Mailhog web UI (catches all outbound email): http://localhost:8025

### Build

```bash
mvn clean package -DskipTests
```

### Test

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=RoleServiceTest

# Single test method
mvn test -Dtest=RoleServiceTest#shouldAssignRole
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `COGNITO_ISSUER_URI` | dev/prod | `https://cognito-idp.<region>.amazonaws.com/<pool-id>` |
| `COGNITO_JWKS_URI` | dev/prod | Cognito JWKS endpoint |
| `COGNITO_USER_POOL_ID` | dev/prod | Cognito User Pool ID |
| `COGNITO_CLIENT_ID` | dev/prod | Cognito App Client ID |
| `COGNITO_CLIENT_SECRET` | optional | App client secret — omit if client has no secret |
| `SPRING_DATASOURCE_URL` | dev/prod | `jdbc:postgresql://<host>:5432/identity_service` |
| `SPRING_DATASOURCE_USERNAME` | dev/prod | DB username |
| `SPRING_DATASOURCE_PASSWORD` | dev/prod | DB password |
| `AWS_REGION` | dev/prod | e.g. `us-east-1` |
| `AWS_ACCESS_KEY_ID` | dev/prod | IAM access key |
| `AWS_SECRET_ACCESS_KEY` | dev/prod | IAM secret key |
| `SES_SMTP_HOST` | dev/prod | SES SMTP endpoint |
| `SES_USERNAME` | dev/prod | SES SMTP username |
| `SES_PASSWORD` | dev/prod | SES SMTP password |

## Profiles

| Profile | DB | Email | Log Level |
|---------|-----|-------|-----------|
| `local` | `localhost:5432` | Mailhog `:1025` | DEBUG |
| `dev` | RDS (env var) | AWS SES | INFO |
| `prod` | RDS (env var) | AWS SES | WARN |

## Infrastructure (Terraform)

```bash
cd terraform/environments/dev
terraform init
terraform plan -var-file="terraform.tfvars"
terraform apply -var-file="terraform.tfvars"
```

Modules:
- `modules/cognito` — User Pool, App Client, groups, MFA config
- `modules/ses` — Domain identity and DNS verification
- `modules/rds` — PostgreSQL instance (dev/prod only)
