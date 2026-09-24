-- Seed RBAC reference data, issue metadata and the default workflow.
-- The bootstrap admin user is created at runtime by DataSeeder using the
-- WORKFORGE_ADMIN_PASSWORD environment variable (never stored in migrations).

-- Permissions
INSERT INTO permissions (name, description) VALUES
    ('USER_MANAGE', 'Manage users'),
    ('USER_READ', 'Read users'),
    ('ORG_MANAGE', 'Manage organizations'),
    ('PROJECT_CREATE', 'Create projects'),
    ('PROJECT_MANAGE', 'Manage projects'),
    ('PROJECT_READ', 'Read projects'),
    ('ISSUE_CREATE', 'Create issues'),
    ('ISSUE_READ', 'Read issues'),
    ('ISSUE_UPDATE', 'Update issues'),
    ('ISSUE_DELETE', 'Delete issues'),
    ('ISSUE_TRANSITION', 'Transition issues'),
    ('ISSUE_ASSIGN', 'Assign issues'),
    ('COMMENT_CREATE', 'Create comments'),
    ('COMMENT_MANAGE', 'Manage comments'),
    ('SPRINT_MANAGE', 'Manage sprints'),
    ('BOARD_MANAGE', 'Manage boards'),
    ('WORKFLOW_MANAGE', 'Manage workflows'),
    ('FILTER_MANAGE', 'Manage filters'),
    ('DASHBOARD_MANAGE', 'Manage dashboards');

-- Roles
INSERT INTO roles (name, description) VALUES
    ('SYSTEM_ADMIN', 'System administrator'),
    ('ORG_ADMIN', 'Organization administrator'),
    ('PROJECT_ADMIN', 'Project administrator'),
    ('PROJECT_MANAGER', 'Project manager'),
    ('DEVELOPER', 'Developer'),
    ('REPORTER', 'Reporter'),
    ('VIEWER', 'Viewer');

-- SYSTEM_ADMIN and ORG_ADMIN get every permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name IN ('SYSTEM_ADMIN', 'ORG_ADMIN');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'PROJECT_MANAGE','PROJECT_READ','PROJECT_CREATE','USER_READ','ISSUE_CREATE','ISSUE_READ',
    'ISSUE_UPDATE','ISSUE_DELETE','ISSUE_TRANSITION','ISSUE_ASSIGN','COMMENT_CREATE','COMMENT_MANAGE',
    'SPRINT_MANAGE','BOARD_MANAGE','WORKFLOW_MANAGE','FILTER_MANAGE','DASHBOARD_MANAGE')
WHERE r.name = 'PROJECT_ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'PROJECT_READ','USER_READ','ISSUE_CREATE','ISSUE_READ','ISSUE_UPDATE','ISSUE_TRANSITION',
    'ISSUE_ASSIGN','COMMENT_CREATE','COMMENT_MANAGE','SPRINT_MANAGE','BOARD_MANAGE',
    'FILTER_MANAGE','DASHBOARD_MANAGE')
WHERE r.name = 'PROJECT_MANAGER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'PROJECT_READ','USER_READ','ISSUE_CREATE','ISSUE_READ','ISSUE_UPDATE','ISSUE_TRANSITION',
    'ISSUE_ASSIGN','COMMENT_CREATE','FILTER_MANAGE','DASHBOARD_MANAGE')
WHERE r.name = 'DEVELOPER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'PROJECT_READ','USER_READ','ISSUE_CREATE','ISSUE_READ','COMMENT_CREATE',
    'FILTER_MANAGE','DASHBOARD_MANAGE')
WHERE r.name = 'REPORTER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.name IN (
    'PROJECT_READ','ISSUE_READ','USER_READ')
WHERE r.name = 'VIEWER';

-- Issue types
INSERT INTO issue_types (name, description, icon, subtask) VALUES
    ('Task', 'A task', 'task', FALSE),
    ('Bug', 'A defect', 'bug', FALSE),
    ('Story', 'A user story', 'story', FALSE),
    ('Epic', 'A large body of work', 'epic', FALSE),
    ('Sub-task', 'A subtask', 'subtask', TRUE);

-- Priorities
INSERT INTO priorities (name, level, color) VALUES
    ('Highest', 1, '#CD1317'),
    ('High', 2, '#E9494A'),
    ('Medium', 3, '#E97F33'),
    ('Low', 4, '#2A8735'),
    ('Lowest', 5, '#57A55A');

-- Statuses
INSERT INTO statuses (name, category) VALUES
    ('To Do', 'TODO'),
    ('In Progress', 'IN_PROGRESS'),
    ('In Review', 'IN_PROGRESS'),
    ('Done', 'DONE');

-- Default workflow
INSERT INTO workflows (name, description, is_default, version, created_at, updated_at)
VALUES ('Default Workflow', 'Standard software development workflow', TRUE, 0, now(), now());

INSERT INTO workflow_statuses (workflow_id, status_id, is_initial, position)
SELECT w.id, s.id, s.name = 'To Do',
       CASE s.name WHEN 'To Do' THEN 0 WHEN 'In Progress' THEN 1
                   WHEN 'In Review' THEN 2 ELSE 3 END
FROM workflows w CROSS JOIN statuses s
WHERE w.name = 'Default Workflow'
  AND s.name IN ('To Do', 'In Progress', 'In Review', 'Done');

INSERT INTO workflow_transitions (workflow_id, from_status_id, to_status_id, name)
SELECT w.id, sf.id, st.id, t.name
FROM workflows w
JOIN (VALUES
        ('To Do', 'In Progress', 'Start Progress'),
        ('In Progress', 'In Review', 'Submit for Review'),
        ('In Review', 'Done', 'Approve'),
        ('In Progress', 'To Do', 'Stop Progress'),
        ('In Review', 'In Progress', 'Reject'),
        ('Done', 'In Progress', 'Reopen')
     ) AS t(from_name, to_name, name) ON TRUE
JOIN statuses sf ON sf.name = t.from_name
JOIN statuses st ON st.name = t.to_name
WHERE w.name = 'Default Workflow';
