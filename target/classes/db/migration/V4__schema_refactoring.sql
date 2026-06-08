-- Optimistic locking version columns
ALTER TABLE roles       ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE permissions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Audit columns for role_permissions (who assigned which permission to which role)
ALTER TABLE role_permissions
    ADD COLUMN assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN assigned_by VARCHAR(255);

-- Drop redundant name column from permissions; (resource, action) is the identity
ALTER TABLE permissions DROP COLUMN name;

ALTER TABLE permissions
    ADD CONSTRAINT uk_permission_resource_action UNIQUE (resource, action);

-- Append-only audit log for all mutable entity changes
CREATE TABLE audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(100) NOT NULL,
    entity_id   VARCHAR(255) NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    actor_sub   VARCHAR(255),
    old_value   JSONB,
    new_value   JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity     ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor      ON audit_logs (actor_sub);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
