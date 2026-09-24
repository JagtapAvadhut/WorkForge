# WorkForge HTTP API

Base path: **`/api/v1`**

Interactive documentation: **`/swagger-ui.html`** (SpringDoc OpenAPI 3).

## Authentication

Unless noted, endpoints require header:

```http
Authorization: Bearer <access_token>
```

### Public auth endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/auth/register` | Create account; returns tokens + user |
| POST | `/auth/login` | Email/password login |
| POST | `/auth/refresh` | Body: `{ "refreshToken": "…" }` — new token pair |
| POST | `/auth/logout` | Revoke refresh token (requires access token) |
| GET | `/auth/me` | Current user profile and authorities |

**Auth response `data`** (`AuthResponse`): `accessToken`, `refreshToken`, `tokenType` (`Bearer`), `expiresIn` (ms), `user`.

Default access token lifetime: 15 minutes (`JWT_ACCESS_EXPIRATION=900000`). Refresh: 7 days (`JWT_REFRESH_EXPIRATION=604800000`).

## Response envelope

Successful controller responses use `ApiResponse<T>`:

```json
{
  "success": true,
  "data": { },
  "message": "Optional human message",
  "timestamp": "2026-01-15T10:00:00Z",
  "traceId": "abc-123"
}
```

Message-only successes set `data` to `null`:

```json
{
  "success": true,
  "data": null,
  "message": "Issue deleted",
  "timestamp": "…",
  "traceId": "…"
}
```

### Errors

Handled exceptions return the same envelope with `success: false` and **`data`** containing `ErrorResponse`:

```json
{
  "success": false,
  "data": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "email", "message": "must be a well-formed email address" }
    ]
  },
  "message": "Validation failed",
  "timestamp": "…",
  "traceId": "…"
}
```

Security filter errors (401/403 before controller) return JSON with `success: false` and a compact `data.code` / `data.message` shape.

Common `code` values align with `ErrorCode` enum (e.g. `UNAUTHORIZED`, `FORBIDDEN`, `INVALID_CREDENTIALS`, `RESOURCE_NOT_FOUND`, `VALIDATION_ERROR`, `OPTIMISTIC_LOCK`, `RESOURCE_CONFLICT`, `INTERNAL_ERROR`).

### Pagination

List endpoints that support paging return `data` as `PageResponse<T>`:

```json
{
  "content": [ ],
  "page": 0,
  "size": 20,
  "totalElements": 100,
  "totalPages": 5,
  "first": true,
  "last": false
}
```

Query parameters: `page`, `size`, `sort` (Spring Data conventions, e.g. `sort=createdAt,desc`).

---

## Organizations

| Method | Path | Permission (typical) |
|--------|------|----------------------|
| POST | `/organizations` | `ORG_MANAGE` |
| GET | `/organizations` | authenticated |
| GET | `/organizations/{id}` | authenticated |
| PUT | `/organizations/{id}` | `ORG_MANAGE` |
| DELETE | `/organizations/{id}` | `ORG_MANAGE` |

## Users

| Method | Path | Notes |
|--------|------|-------|
| POST | `/users` | `USER_MANAGE` |
| GET | `/users` | Paginated list; `USER_READ` |
| GET | `/users/{id}` | `USER_READ` |
| PUT | `/users/{id}` | `USER_MANAGE` |
| PATCH | `/users/{id}/status` | `USER_MANAGE` |
| DELETE | `/users/{id}` | Soft disable; `USER_MANAGE` |

## Projects

| Method | Path | Notes |
|--------|------|-------|
| POST | `/projects` | `PROJECT_CREATE` |
| GET | `/projects` | Paginated; `PROJECT_READ` |
| GET | `/projects/{id}` | `PROJECT_READ` |
| PUT | `/projects/{id}` | `PROJECT_MANAGE` |
| DELETE | `/projects/{id}` | Disable project |
| POST | `/projects/{id}/members` | Add member |
| GET | `/projects/{id}/members` | List members |
| DELETE | `/projects/{id}/members/{userId}` | Remove member |

Creating a project initializes the issue sequence row and associates a workflow.

## Issues

| Method | Path | Notes |
|--------|------|-------|
| POST | `/issues` | Create; `ISSUE_CREATE` |
| GET | `/issues/search` | Query param `jql` + pagination; `ISSUE_READ` |
| GET | `/issues/{id}` | By numeric id |
| GET | `/issues/key/{key}` | By issue key (e.g. `DEMO-1`) |
| GET | `/issues/{id}/subtasks` | Child issues |
| PUT | `/issues/{id}` | Update fields; `ISSUE_UPDATE` |
| PATCH | `/issues/{id}/assignee` | Body: `{ "assigneeId": … }` |
| PATCH | `/issues/{id}/status` | Body: `{ "statusId": … }` |
| PATCH | `/issues/{id}/priority` | Body: `{ "priorityId": … }` |
| PATCH | `/issues/{id}/sprint` | Body: `{ "sprintId": … }` |
| POST/DELETE | `/issues/{id}/labels/{labelId}` | Label links |
| POST/DELETE | `/issues/{id}/components/{componentId}` | Component links |
| DELETE | `/issues/{id}` | `ISSUE_DELETE` |
| POST/DELETE | `/issues/{id}/watch` | Watchers |
| GET | `/issues/{id}/watchers` | User ids |
| POST | `/issues/{id}/comments` | Body: `{ "body": "…" }` |
| GET | `/issues/{id}/comments` | Paginated |
| PUT | `/issues/comments/{commentId}` | Update comment |
| DELETE | `/issues/comments/{commentId}` | Delete comment |

### JQL search grammar

`GET /issues/search?jql=project=DEMO AND status=Open`

- Clauses: `field op value`, combined with `AND` (case-insensitive).
- Operators: `=`, `!=`, `~` (contains; applies to summary semantics).
- Fields: `project` (project key), `status`, `priority`, `type`, `assignee`, `reporter`, `sprint`, `summary`.
- Values may be quoted strings or numeric ids where applicable.

## Sprints

Prefix: `/sprints`

| Method | Path | Notes |
|--------|------|-------|
| POST | `/sprints` | `SPRINT_MANAGE` |
| GET | `/sprints?projectId=` | List for project |
| PUT | `/sprints/{id}` | Update |
| POST | `/sprints/{id}/start` | Start sprint |
| POST | `/sprints/{id}/complete` | Complete sprint |
| GET | `/sprints/{id}/issues` | Issues in sprint |
| GET | `/sprints/backlog?projectId=` | Unscheduled issues |

## Comments (issue-scoped)

Comments are exposed under `/issues/{id}/comments` (see above). There is no separate top-level comments resource.

## Notifications

Prefix: `/notifications`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/notifications` | Paginated inbox for current user |
| GET | `/notifications/unread-count` | `{ "count": N }` in `data` |
| PATCH | `/notifications/{id}/read` | Mark one read |
| PATCH | `/notifications/read-all` | Mark all read |

## Related resources (summary)

| Prefix | Purpose |
|--------|---------|
| `/workflows/{id}/statuses` | Workflow status catalog |
| `/workflows/{id}/transitions?fromStatusId=` | Allowed transitions |
| `/boards` | Kanban boards and views |
| `/labels`, `/components` | Project-scoped metadata |
| `/issues/{id}/attachments` | Multipart upload; download by attachment id |
| `/filters` | Saved JQL filters |
| `/dashboards` | Personal/shared dashboards |

Refer to Swagger for request/response DTO field lists and validation rules.

## Health and metadata

| Path | Auth |
|------|------|
| `/actuator/health/**` | Public |
| `/actuator/info` | Public |
| `/actuator/metrics`, `/actuator/prometheus` | Authenticated (default Spring Boot rules) |
