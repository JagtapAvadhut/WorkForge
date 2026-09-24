# Evaluation (Phase 12)

## What is being measured

WorkForge AI evaluation runs a fixed catalog of cases against **real** Phase 1–11 services:

| Category | Example check |
|----------|----------------|
| CHAT | Non-empty response |
| RAG | Answer exists; sources or no-context path |
| TOOL | Response exists; expected tool when tools were used |
| AGENT | Completes / max_steps; iterations within limit |
| GRAPH | `completed=true` |
| MCP | Allowlisted tools discovered; server up |
| MEMORY | Conversation stores ≥4 messages across two turns |
| MULTI_AGENT | Expected agents (e.g. ISSUE_AGENT) or safe completion |

API: `POST /api/v1/ai/evaluation/run` with `{ "suite": "ALL" }` (or a category name).

## What is deterministic

Pass/fail uses structural checks:

- tools / agents selected
- completion / stop status
- message counts
- MCP discovery
- non-empty answers

**Not** exact natural-language equality.

## Limitations of LLM evaluation

- Model wording varies; cases avoid brittle string matches.
- Tool cases may pass on response-only if the model skips tools (documented expectation is soft when no tools fire).
- Full `ALL` suite is slow because it calls Ollama for live cases.
- This is a learning harness, not an official benchmark suite.

UI: `/evaluation`
