INSERT INTO roles (name, description) VALUES
    ('ADMIN',    'Full system access'),
    ('USER',     'Standard user access'),
    ('READONLY', 'Read-only access across all resources');

INSERT INTO permissions (name, resource, action) VALUES
    ('users:read',        'users',   'read'),
    ('users:write',       'users',   'write'),
    ('users:delete',      'users',   'delete'),
    ('roles:read',        'roles',   'read'),
    ('roles:write',       'roles',   'write'),
    ('roles:delete',      'roles',   'delete'),
    ('permissions:read',  'permissions', 'read'),
    ('permissions:write', 'permissions', 'write');

-- ADMIN gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN';

-- USER gets read on users only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.name IN ('users:read')
WHERE r.name = 'USER';

-- READONLY gets all read permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.action = 'read'
WHERE r.name = 'READONLY';
