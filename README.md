# WorkForge

WorkForge is an enterprise project management and issue tracking platform. The monorepo contains a **Spring Boot** REST API (`backend`) and a **React + Vite** SPA (`frontend`), backed by **PostgreSQL** with schema managed by **Flyway** (migrations `V1`–`V12`).

## Overview

WorkForge supports organizations, projects, issues with workflows, sprints, Kanban boards, comments, attachments, saved filters, dashboards, in-app notifications, and audit logging. Authentication uses JWT access and refresh tokens with role-based access control (RBAC).

## Architecture

```
┌─────────────┐     HTTPS/HTTP      ┌──────────────────┐     JDBC      ┌────────────┐
│  React SPA  │ ──────────────────► │  Spring Boot API │ ────────────► │ PostgreSQL │
│  (Vite)     │   /api/v1 + Bearer  │  com.avadhoot…   │               │  (Flyway)  │
└─────────────┘                     └──────────────────┘               └────────────┘
```

In local development, the Vite dev server proxies `/api` to `http://localhost:8080`. See [docs/architecture.md](docs/architecture.md) for module boundaries and data flow.

## Technology stack

| Layer | Technology |
|-------|------------|
| Backend | Java 21, Spring Boot 4.1.1, Spring Security, Spring Data JPA, Flyway, MapStruct, JJWT |
| API docs | SpringDoc OpenAPI 3 — UI at `/swagger-ui.html`, spec at `/v3/api-docs` |
| Frontend | React 18, TypeScript, Vite 6, TanStack Query, Zustand, Tailwind CSS, React Router |
| Database | PostgreSQL 16 (recommended) |
| Optional infra | Docker Compose provides PostgreSQL and Redis; the backend does **not** require Docker if PostgreSQL is installed locally |

## Prerequisites

- **JDK 21**
- **Maven** (or use the included wrapper: `backend/mvnw` / `backend/mvnw.cmd`)
- **Node.js 20+** and npm (for the frontend)
- **PostgreSQL 16** (local install or via `docker compose up -d postgres`)

## PostgreSQL setup

### Option A: Docker Compose (optional)

From the repository root:

```bash
docker compose up -d postgres
```

This creates database `workforge` with user `postgres`. The compose file defaults `POSTGRES_PASSWORD` to `postgres` for **local development only** — override in production.

### Option B: Local PostgreSQL

Create the database:

```sql
CREATE DATABASE workforge;
```

Ensure credentials match your environment variables (see below). Defaults in `application-dev.yml` are host `localhost`, port `5432`, database `workforge`, user `postgres`, password `postgres`.

## Environment variables

Backend configuration is driven by `backend/src/main/resources/application.yml` (shared) and `application-dev.yml` (datasource, active when profile `dev` is used). Copy `backend/.env.example` and export variables or use your IDE run configuration.

| Variable | Default (dev) | Description |
|----------|---------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Spring profile |
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `workforge` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | dev placeholder | **Required in production** — long random secret for HMAC |
| `JWT_ACCESS_EXPIRATION` | `900000` | Access token TTL (ms) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Refresh token TTL (ms) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Allowed browser origins |
| `FILE_STORAGE_LOCATION` | `./data/attachments` | Local path for uploaded files |
| `WORKFORGE_ADMIN_EMAIL` | `admin@workforge.local` | Bootstrap admin email |
| `WORKFORGE_ADMIN_USERNAME` | `admin` | Bootstrap admin username |
| `WORKFORGE_ADMIN_PASSWORD` | *(empty)* | **Required** to create the bootstrap admin user on first startup |
| `RATE_LIMIT_CAPACITY` | `100` | Rate limit bucket size |
| `RATE_LIMIT_REFILL_TOKENS` | `100` | Tokens refilled per period |
| `RATE_LIMIT_REFILL_PERIOD_SECONDS` | `60` | Refill period |

Frontend: see `frontend/.env.example` (`VITE_API_BASE_URL`).

## Run the backend

```bash
cd backend
# Windows
set WORKFORGE_ADMIN_PASSWORD=your-chosen-admin-password
mvnw.cmd spring-boot:run

# Linux/macOS
export WORKFORGE_ADMIN_PASSWORD=your-chosen-admin-password
./mvnw spring-boot:run
```

Flyway runs automatically on startup (`spring.flyway.enabled=true` in the dev profile). Hibernate `ddl-auto` is `validate` — schema changes must be added as new Flyway migrations under `backend/src/main/resources/db/migration/`.

API base URL: `http://localhost:8080/api/v1`

## Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Dev server: `http://localhost:5173` (proxies `/api` to the backend).

Production build:

```bash
npm run build
npm run preview
```

## Database migrations

| Version | Script | Purpose |
|---------|--------|---------|
| V1 | `V1__core_auth_rbac.sql` | Organizations, users, roles, permissions, refresh tokens |
| V2 | `V2__projects.sql` | Projects, members, issue sequence counter |
| V3 | `V3__issue_metadata.sql` | Issue types, priorities, statuses |
| V4 | `V4__workflows.sql` | Workflows, statuses, transitions |
| V5 | `V5__issues.sql` | Issues |
| V6 | `V6__labels.sql` | Labels |
| V7 | `V7__components.sql` | Components |
| V8 | `V8__sprints.sql` | Sprints |
| V9 | `V9__boards.sql` | Boards and columns |
| V10 | `V10__comments_attachments_watchers.sql` | Comments, attachments, watchers |
| V11 | `V11__notifications_filters_dashboards_audit.sql` | Notifications, filters, dashboards, audit |
| V12 | `V12__seed_reference_data.sql` | Reference metadata (not admin password) |

Details: [docs/database.md](docs/database.md).

## Default admin user

On first startup, `DataSeeder` creates a user with role `SYSTEM_ADMIN` when:

1. No user exists with username `WORKFORGE_ADMIN_USERNAME` (default `admin`), and  
2. `WORKFORGE_ADMIN_PASSWORD` is set to a non-empty value.

If the password is not set, startup continues but logs a warning and **no** admin user is created. The password is never stored in SQL migrations.

## Swagger and Actuator

- **OpenAPI UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Actuator** (exposed): `health`, `info`, `metrics`, `prometheus`
  - Public without auth: `/actuator/health/**`, `/actuator/info`
  - Health details: `when_authorized` for other actuator endpoints

## Testing

**Backend** (H2 in-memory, Flyway disabled — see `application-test.yml`):

```bash
cd backend
./mvnw test
# Windows: mvnw.cmd test
```

**Frontend**:

```bash
cd frontend
npm test
```

## Repository structure

```
IVL/
├── backend/                 # Spring Boot API (com.avadhoot.workforge)
│   ├── src/main/java/       # Domain modules: auth, project, issue, workflow, …
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   └── db/migration/    # Flyway V1–V12
│   └── mvnw, mvnw.cmd
├── frontend/                # React SPA
│   └── src/
│       ├── api/             # Axios clients
│       ├── pages/           # Route pages
│       └── components/
├── docs/                    # Architecture, API, database, development
└── docker-compose.yml       # Optional PostgreSQL + Redis
```

Further reading: [docs/api.md](docs/api.md), [docs/development.md](docs/development.md).

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Flyway / startup fails on schema | DB unreachable or wrong credentials | Check `DB_*` vars and that PostgreSQL is running |
| `validate` / migration errors | DB out of sync with code | Run against a fresh DB or repair Flyway history carefully |
| Cannot log in as admin | `WORKFORGE_ADMIN_PASSWORD` was unset on first run | Set password and delete the partial state, or create a user via register + DB role assignment |
| 401 on API calls | Missing or expired JWT | Login or refresh; frontend stores tokens in memory/local storage via `authStore` |
| CORS errors from browser | Origin not allowed | Add your frontend URL to `CORS_ALLOWED_ORIGINS` |
| Upload failures | Permissions or path | Ensure `FILE_STORAGE_LOCATION` exists and is writable |
| Port in use | Another process on 8080 or 5173 | Set `SERVER_PORT` or change Vite port in `vite.config.ts` |

## License

See repository license file if present.
"# WorkForge" 
