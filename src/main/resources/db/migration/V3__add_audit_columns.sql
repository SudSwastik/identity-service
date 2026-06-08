-- roles: already has created_at, add remaining audit columns
ALTER TABLE roles
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMPTZ,
    ADD COLUMN modified_by VARCHAR(255);

-- permissions: no audit columns yet, add all four
ALTER TABLE permissions
    ADD COLUMN created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN created_by  VARCHAR(255),
    ADD COLUMN modified_at TIMESTAMPTZ,
    ADD COLUMN modified_by VARCHAR(255);

-- user_roles: add assigned_by alongside existing assigned_at
ALTER TABLE user_roles
    ADD COLUMN assigned_by VARCHAR(255);
