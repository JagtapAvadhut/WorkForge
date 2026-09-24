# WorkForge AI — Demo Guide

End-to-end demo for Phases 1–12. No fake frontend data. Real Ollama, PGVector, MCP, and PostgreSQL (`workforge_ai`).

## 1. Prerequisites

| Dependency | Notes |
|------------|--------|
| JDK 21 + Maven | AI backend + MCP server |
| Node 20+ | AI frontend |
| PostgreSQL | Database `workforge_ai` with `pgvector` |
| Ollama | Chat + embedding models (see `application-dev.yml`) |
| WorkForge API (optional) | `:8080` with MWS sample issues for tool/MCP demos |

## 2. Required services

| Service | Port | Role |
|---------|------|------|
| AI backend | **8090** | Chat, RAG, tools, agents, eval, security, observability |
| MCP server | **8091** | MCP tool discovery / execution |
| AI frontend | Vite (e.g. 5174) | UI tabs |
| Ollama | 11434 | LLM + embeddings |
| PostgreSQL | 5432 | `workforge_ai` |

## 3. Startup commands

From `workforge-ai/`:

```powershell
# Terminal 1 — AI backend
cd ai-backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# Terminal 2 — MCP server
cd mcp-server
.\mvnw.cmd spring-boot:run

# Terminal 3 — AI frontend
cd ai-frontend
npm install
npm run dev
```

Confirm:

- `GET http://localhost:8090/api/v1/ai/security/policies`
- `GET http://localhost:8090/api/v1/ai/mcp/status`

## 4. Demo seed

Idempotent seed (documents + preference memory):

```powershell
cd workforge-ai
.\scripts\seed-ai-demo.ps1
```

Or from the UI: **Documents → Load demo documents**, **Memory → Load demo memory**.

API:

```http
POST /api/v1/ai/demo/seed?sessionId=demo-session
```

## 5. Smoke tests

```powershell
.\scripts\smoke-test-ai.ps1
# Faster (skip live Ollama-heavy paths):
.\scripts\smoke-test-ai.ps1 -SkipSlow
```

Exit code is non-zero if a critical check fails. Body shape is validated, not only HTTP 200.

## 6. Tabs — sample inputs & expected behavior

### Chat

- **Purpose:** Prompt strategies + conversation memory
- **Samples:** Explain sprint / RAG / Agent vs Tool Calling / MCP / embeddings
- **API:** `POST /api/v1/ai/chat`
- **Expected:** Non-empty model response; conversationId when memory enabled

### Embeddings

- **Purpose:** Vectors, cosine similarity, ranking
- **Samples:** Pre-filled demo texts; **Load Demo** resets them
- **API:** `POST /ai/embeddings`, `/similarity`, `/compare`
- **Expected:** Dimensions > 0; high similarity for sprint paraphrases; sprint doc ranks first

### Documents

- **Purpose:** PGVector ingest + semantic search
- **Action:** **Load demo documents** (idempotent via `demoKey`)
- **API:** `POST /ai/demo/seed`, `GET /ai/documents`, `POST /ai/documents/search`
- **Expected:** ≥10 knowledge docs; search returns relevant hits

### RAG

- **Purpose:** Retrieve then generate
- **Presets:** sprint, backlog, workflow, RAG, issue + unknown payroll question
- **API:** `POST /ai/rag/query`
- **Expected:** Sources for known questions; no-context style answer for payroll

### Tools

- **Purpose:** Read-only `getIssue` / `searchIssues` / `getProject`
- **Samples:** Get MWS-1; open MWS issues; project MWS; assignee Avadhoot
- **API:** `POST /ai/tool-chat`, `POST /ai/tool-preview`
- **Expected:** Real tool calls + final response (needs WorkForge + MWS data)

### Agent

- **Purpose:** Multi-step tool loop
- **Presets:** sprint (no tool); MWS-1; project+issues; investigate MWS-1
- **API:** `POST /ai/agent/run`
- **Expected:** Visible safe execution steps; max steps default 5

### Graph

- **Purpose:** LangGraph nodes + conditional edges
- **Presets:** sprint; MWS-1+project; open issues; investigate
- **API:** `POST /ai/graph-agent`
- **Expected:** Timeline of nodes (ANALYZE → DECIDE → TOOL → … → FINALIZE); maxIterations=5

### MCP

- **Purpose:** Protocol tools via MCP server
- **Samples:** Same MWS prompts; **Refresh tools**
- **API:** `GET /ai/mcp/status`, `GET /ai/mcp/tools`, MCP chat/agent endpoints
- **Expected:** Real UP/CONNECTED or clean DOWN — never fake CONNECTED

### Memory

- **Purpose:** Short-term conversations + long-term preferences
- **Demo:** Load demo memory → preference “simple explanations” → ask via Chat/Agent
- **API:** `/ai/memory/*`
- **Expected:** History used on follow-ups; loaded memories listed; no chain-of-thought stored

### Multi-Agent

- **Purpose:** Supervisor + specialists
- **Presets:** Investigate MWS-1 + docs; MWS project/issues; MWS-1 + documentation
- **API:** `POST /ai/multi-agent`
- **Expected:** Agent selection, summaries, sources/tools, final answer (no private reasoning)

### Evaluation

- **Purpose:** Structural regression of Phase 1–11 capabilities
- **Suites:** `SMOKE` (fast), `CORE`, `ALL`
- **API:**
  - `POST /ai/evaluation/run` → `{ runId, status: QUEUED|RUNNING, … }` immediately
  - `GET /ai/evaluation/runs/{runId}` → progress + partial/final results
- **UI:** Polls every 1s; shows Progress `n / total`, Current, Elapsed, Pass/Fail table
- **Expected:** SMOKE finishes quickly; ALL never fails due to browser HTTP timeout

### Security

- **Purpose:** Heuristic SAFE / SUSPICIOUS / BLOCKED + policy display
- **Samples:** Safe RAG question; reveal system prompt; execute any tool; secret-like JWT/password
- **API:** `POST /ai/security/check`, `GET /ai/security/policies`
- **Expected:** Correct classification labels; not claimed to be perfect

### Observability

- **Purpose:** Real traces (no invented tokens)
- **Actions:** Refresh; **Run demo request** (`Investigate MWS-1`)
- **API:** `/ai/observability/summary`, `/traces`, `/traces/{id}`
- **Expected:** Real latency/success/tool/agent/MCP/RAG counts; tokens only if provider exposes them

## 7. Evaluation suites

| Suite | Cases | Notes |
|-------|-------|--------|
| SMOKE | MCP tools + 2 security checks | Minimal / no heavy Ollama |
| CORE | SMOKE + chat, RAG, agent, multi-agent | Representative live checks |
| ALL | Full catalog | Async job + per-case timeouts + partial results |

## 8. API examples

```http
POST /api/v1/ai/evaluation/run
Content-Type: application/json

{ "suite": "SMOKE" }
```

```http
GET /api/v1/ai/evaluation/runs/{runId}
```

```http
POST /api/v1/ai/demo/seed?sessionId=demo-session
```

## 9. Troubleshooting

| Symptom | Check |
|---------|--------|
| Evaluation “timeout of 120000ms” | Use async UI (poll). Do not hold one HTTP request for ALL. |
| MCP DISCONNECTED | Start `mcp-server` on :8091; click Refresh tools |
| RAG empty sources | Run seed script / Load demo documents |
| Tool/MCP can’t find MWS-1 | Start main WorkForge with sample MWS data (AI DB unchanged) |
| Embeddings fail | Ollama running + embedding model pulled |
| Token fields null | Expected when Ollama does not return usage |

## 10. What this guide does **not** do

- Does not redesign architecture
- Does not add a new AI phase
- Does not modify the main WorkForge/Jira app or its database
- Does not invent success responses or token/cost metrics
