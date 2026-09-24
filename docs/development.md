# Local development workflow

This guide assumes you work from the repository root `IVL/` on a machine with JDK 21, Node.js, and PostgreSQL (or Docker Compose for Postgres only).

## 1. Clone and branch

Use your team’s Git workflow. The backend artifact is `com.avadhoot:workforge:0.1.0-SNAPSHOT`.

## 2. Start infrastructure (optional)

Docker is **not** required to run the Spring Boot app if PostgreSQL is installed locally.

```bash
docker compose up -d postgres
```

Redis in compose is reserved for future features; the current backend does not connect to Redis.

## 3. Configure environment

### Backend

Copy and customize:

```bash
cp backend/.env.example backend/.env
```

Load variables into your shell or IDE run configuration. Minimum for a usable dev setup:

- `DB_*` matching your PostgreSQL instance
- `WORKFORGE_ADMIN_PASSWORD` set before **first** startup (creates admin user)
- `JWT_SECRET` — change from default before any shared/staging deploy

Spring profile defaults to `dev` via `SPRING_PROFILES_ACTIVE`.

### Frontend

```bash
cp frontend/.env.example frontend/.env
```

Default `VITE_API_BASE_URL=/api/v1` works with the Vite dev proxy.

## 4. Run backend

```bash
cd backend
./mvnw spring-boot:run          # Unix
mvnw.cmd spring-boot:run        # Windows
```

Verify:

- `GET http://localhost:8080/actuator/health`
- Open `http://localhost:8080/swagger-ui.html`

Flyway applies migrations on startup. Logs at `DEBUG` for `com.avadhoot.workforge` in dev.

## 5. Run frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. Log in with the seeded admin (if `WORKFORGE_ADMIN_PASSWORD` was set) or register a new user.

## 6. Day-to-day tasks

| Task | Command / location |
|------|---------------------|
| Backend tests | `cd backend && ./mvnw test` |
| Frontend tests | `cd frontend && npm test` |
| Frontend lint | `npm run lint` |
| API exploration | Swagger UI or `frontend/src/api/*` clients |
| New DB change | Add `V{n}__description.sql` under `backend/src/main/resources/db/migration/` |
| Attachments | Files under `./data/attachments` (gitignored) |

## 7. Coding conventions

- **Backend**: Package by domain (`issue`, `project`, `auth`, …). Use DTOs for API; keep entities in `domain` packages. Prefer `@PreAuthorize` on controllers for permission checks.
- **Frontend**: Path alias `@/` → `src/`. Co-locate API modules in `src/api/`. Use TanStack Query for server state; Zustand for auth/UI tokens.
- **Errors**: Surface `ApiRequestError` from axios interceptors; backend codes in `ErrorResponse.code`.

## 8. Debugging tips

- **401 loops**: Check refresh token expiry and clock skew; clear browser storage if tokens are corrupt.
- **403 on admin pages**: User needs `SYSTEM_ADMIN` or relevant permission; check `/auth/me` authorities.
- **Optimistic lock**: Retry after concurrent edit (`OPTIMISTIC_LOCK` response).
- **Search empty**: Confirm JQL field names and project key exist; empty `jql` returns unrestricted list (subject to permissions).

## 9. Production differences

- Set strong `JWT_SECRET` and database credentials via secrets manager.
- Set `SPRING_PROFILES_ACTIVE` to a production profile when you add `application-prod.yml` (not shipped in this repo snapshot).
- Serve frontend static assets from CDN or reverse proxy; point `VITE_API_BASE_URL` at the public API origin.
- Do not rely on compose default `POSTGRES_PASSWORD=postgres` outside local machines.

## 10. Documentation map

- [README.md](../README.md) — setup and operations
- [architecture.md](architecture.md) — system design
- [database.md](database.md) — schema and sequences
- [api.md](api.md) — REST reference
