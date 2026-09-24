-- Phase 6: controlled WorkForge-style dataset for read-only tool calling
CREATE TABLE IF NOT EXISTS wf_projects (
    id           BIGSERIAL PRIMARY KEY,
    project_key  VARCHAR(32) NOT NULL UNIQUE,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    lead_username VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS wf_issues (
    id            BIGSERIAL PRIMARY KEY,
    issue_key     VARCHAR(32) NOT NULL UNIQUE,
    project_key   VARCHAR(32) NOT NULL REFERENCES wf_projects(project_key),
    summary       VARCHAR(500) NOT NULL,
    status        VARCHAR(64) NOT NULL,
    priority      VARCHAR(32) NOT NULL,
    assignee      VARCHAR(100),
    issue_type    VARCHAR(64) NOT NULL,
    sprint_name   VARCHAR(120),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_wf_issues_project_status ON wf_issues (project_key, status);
CREATE INDEX IF NOT EXISTS idx_wf_issues_assignee ON wf_issues (assignee);

INSERT INTO wf_projects (project_key, name, description, lead_username)
VALUES
    ('MWS', 'Mobile Web Store', 'WorkForge AI sample project for tool-calling demos.', 'avadhoot'),
    ('OPS', 'Operations Hub', 'Internal ops tracking project for sample data.', 'priya')
ON CONFLICT (project_key) DO NOTHING;

INSERT INTO wf_issues (issue_key, project_key, summary, status, priority, assignee, issue_type, sprint_name)
VALUES
    ('MWS-1', 'MWS', 'Fix checkout button alignment on mobile', 'OPEN', 'HIGH', 'avadhoot', 'Bug', 'Sprint 12'),
    ('MWS-2', 'MWS', 'Add product filter by brand', 'IN_PROGRESS', 'MEDIUM', 'avadhoot', 'Story', 'Sprint 12'),
    ('MWS-3', 'MWS', 'Improve search latency under load', 'OPEN', 'HIGHEST', 'neha', 'Task', 'Sprint 12'),
    ('MWS-4', 'MWS', 'Update payment gateway docs', 'DONE', 'LOW', 'priya', 'Task', 'Sprint 11'),
    ('MWS-5', 'MWS', 'Investigate cart session expiry', 'OPEN', 'MEDIUM', 'avadhoot', 'Bug', NULL),
    ('OPS-1', 'OPS', 'Rotate staging credentials', 'OPEN', 'HIGH', 'priya', 'Task', 'Ops Sprint 3'),
    ('OPS-2', 'OPS', 'Document on-call runbook', 'IN_PROGRESS', 'MEDIUM', 'avadhoot', 'Story', 'Ops Sprint 3')
ON CONFLICT (issue_key) DO NOTHING;
