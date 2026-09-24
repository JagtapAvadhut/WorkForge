# WorkForge database

PostgreSQL schema is owned by Flyway migrations in `backend/src/main/resources/db/migration/`. Hibernate validates the schema at startup (`ddl-auto: validate` in dev).

## Entity relationship overview

```
organizations ──┬── users ──┬── user_roles ── roles ── role_permissions ── permissions
                │           │
                │           └── refresh_tokens
                │
                └── projects ──┬── project_members
                                 ├── project_issue_seq  (1:1 counter per project)
                                 ├── sprints
                                 ├── labels / components (scoped by project)
                                 ├── boards ── board_columns
                                 └── issues ──┬── comments
                                              ├── attachments
                                              ├── watchers
                                              ├── issue_labels / issue_components (joins)
                                              └── parent_id → issues (subtasks)

workflows ── workflow_statuses ── statuses (global catalog from V3)
         └── workflow_transitions

users ── notifications
     └── saved_filters / dashboards ── dashboard_gadgets

audit_logs (append-only, no FK to entities)
```

## Core tables

### Identity and RBAC (V1)

| Table | Role |
|-------|------|
| `organizations` | Tenant boundary; optional link from users and projects |
| `users` | Credentials, profile, `status`, soft `enabled` |
| `roles`, `permissions` | Named RBAC catalog |
| `user_roles`, `role_permissions` | Many-to-many grants |
| `refresh_tokens` | Hashed refresh token storage with expiry and revoke flag |

**Indexes**: `idx_users_organization`, `idx_refresh_tokens_user`.

### Projects (V2)

| Table | Role |
|-------|------|
| `projects` | `project_key` (unique, short prefix), name, `workflow_id`, `lead_id`, org link |
| `project_members` | `(project_id, user_id)` unique; project role string |
| `project_issue_seq` | One row per project: `seq` counter for issue numbers |

**Indexes**: unique constraints on `project_key`, `project_members`.

### Issue metadata (V3)

Global catalogs referenced by issues and workflows:

- `issue_types`, `priorities`, `statuses` (with ordering/display metadata)

### Workflows (V4)

| Table | Role |
|-------|------|
| `workflows` | Named workflow; `is_default` flag |
| `workflow_statuses` | Which statuses belong to a workflow and which is initial |
| `workflow_transitions` | Directed edges `(from_status_id → to_status_id)` with transition name |

**Index**: `idx_workflow_transitions_lookup (workflow_id, from_status_id, to_status_id)`.

### Issues (V5–V7)

| Table | Role |
|-------|------|
| `issues` | Core work item: key, summary, type/priority/status FKs, reporter/assignee, optional parent, sprint, story points, due date |
| `labels`, `components` | Project-scoped tags and modules |
| Join tables | Link issues to labels and components |

**Indexes on `issues`**: `project_id`, `status_id`, `assignee_id`, `sprint_id`, `parent_id`; unique `issue_key`.

### Sprints and boards (V8–V9)

| Table | Role |
|-------|------|
| `sprints` | `state` (e.g. FUTURE/ACTIVE/COMPLETE), dates, goal |
| `boards`, `board_columns` | Kanban configuration; columns map to status IDs |

### Collaboration (V10)

| Table | Role |
|-------|------|
| `comments` | Thread on issue; optimistic `version` |
| `attachments` | Metadata + `storage_path` (bytes on filesystem) |
| `watchers` | Unique `(issue_id, user_id)` |

**Indexes**: `idx_comments_issue`, `idx_attachments_issue`.

### Productivity and audit (V11)

| Table | Role |
|-------|------|
| `notifications` | Per-user inbox |
| `saved_filters` | JQL string + shared flag |
| `dashboards`, `dashboard_gadgets` | User dashboards |
| `audit_logs` | Entity/action audit trail |

**Indexes**: `idx_notifications_user (user_id, is_read)`, `idx_audit_logs_entity (entity_type, entity_id)`.

### Seed data (V12)

Reference rows for types, priorities, statuses, and related metadata. **Does not** insert the bootstrap admin user — that is runtime-only via `WORKFORGE_ADMIN_PASSWORD`.

## Issue key and sequence strategy

Human-readable keys follow **`{PROJECT_KEY}-{NUMBER}`** (e.g. `DEMO-42`).

1. When a **project** is created, `IssueSequenceService.initialise(projectId)` inserts `project_issue_seq` with `seq = 0` if missing.
2. On **issue create**, `IssueSequenceService.next(projectId)` runs in the same transaction:
   - `SELECT … FOR UPDATE` on the project’s sequence row (pessimistic lock).
   - Increment `seq`, persist, return the new value.
3. `IssueService` sets `issue_key = project.getKey() + "-" + seq`.

Properties:

- **Per-project** counters (not global).
- **Concurrency-safe** under parallel creates (serialized on the sequence row).
- **Gap-free** within a project for successful commits (failed transactions roll back the increment).

Unique constraint on `issues.issue_key` prevents duplicates even if application logic regresses.

## Indexing strategy

Indexes favor the hottest query paths:

- Issue lists filtered by **project**, **status**, **assignee**, **sprint**, **parent** (subtasks).
- Notification inbox by **user + read state**.
- Audit history by **entity type and id**.
- Workflow transition lookup by **workflow + from + to** status.

Full-text search is not implemented at the DB layer; summary search uses JPA specifications (`summary` field / `~` operator in JQL).

## Migrations and changes

- Never edit applied migration files in shared environments.
- Add `V13__…sql` (or next version) for schema changes.
- Keep seed/reference changes separate from secrets (no passwords in SQL).

## Connection defaults (dev)

From `application-dev.yml`:

- URL: `jdbc:postgresql://localhost:5432/workforge`
- Pool: Hikari max 10 connections
- JDBC time zone: UTC
