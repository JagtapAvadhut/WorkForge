# WorkForge AI — Multi-Agent (Phase 11)

Controlled multi-agent orchestration on **one** WorkForge/Jira dataset. Multiple specialists do not mean multiple Jira instances.

## 1. What multi-agent means

A **Supervisor** coordinates specialized agents. Each specialist owns a capability (issues, knowledge, projects). The Supervisor delegates, collects structured results, and synthesizes the final answer.

## 2. Why use multiple agents

- Clear responsibility boundaries
- Reuse Phase 5 RAG, Phase 6/9 tools/MCP, Phase 10 memory without duplicating logic
- Safer allowlists per specialist
- Better routing than one mega-prompt

## 3. Supervisor agent

- Understands the user request
- Decides which specialists are needed
- Does **not** call repositories or tools
- May finalize directly for conceptual questions
- May re-delegate after reviewing results
- Stops at max supervisor iterations (default **5**)

## 4. Specialist agents

| Agent | Role | Backend |
|-------|------|---------|
| `ISSUE_AGENT` | getIssue / searchIssues | MCP (fallback local tools) |
| `KNOWLEDGE_AGENT` | grounded docs | Phase 5 `RagService` |
| `PROJECT_AGENT` | getProject + issue summary | MCP (fallback local tools) |

Optional `ExternalDataAgent` interface exists for a future free/public API — not wired by default.

## 5. Shared state

`MultiAgentState` (per invoke only):

- user request, conversation id
- supervisor decisions, delegated tasks
- specialist results, sources, tool calls, steps
- pending agents, iteration / specialist call counts
- final answer, completed, status

No hidden chain-of-thought persistence.

## 6. Agent delegation

Supervisor JSON:

```json
{"action":"delegate","agents":["ISSUE_AGENT","KNOWLEDGE_AGENT"],"task":"..."}
```

or

```json
{"action":"finalize","answer":"..."}
```

Multiple agents in one decision run as an independent batch (sequential nodes without returning to the Supervisor between them), then results return to the Supervisor.

## 7. Agent results

```json
{
  "agent": "ISSUE_AGENT",
  "status": "SUCCESS",
  "summary": "...",
  "findings": ["..."],
  "sources": [],
  "toolCalls": []
}
```

## 8. LangGraph orchestration

```
START → SUPERVISOR
         ├─ ISSUE_AGENT ──┐
         ├─ KNOWLEDGE ────┼→ next pending specialist OR SUPERVISOR
         ├─ PROJECT ──────┘
         └─ FINALIZE → END
```

Limits: `workforge.ai.multi-agent.max-iterations` (5), `max-specialist-calls` (10).

## 9. Memory

Phase 10 `ConversationMemoryFacade`: prepare → multi-agent → completeTurn (user/assistant only).

## 10. RAG

Only through `KnowledgeRagAgent` → `RagService`. No duplicated vector logic.

## 11. MCP

Issue/Project specialists prefer `McpClientGateway`. If MCP is down, read-only Phase 6 tools are used as fallback. Allowlist: getIssue, searchIssues, getProject.

## 12. Single agent vs multi-agent

| | Single agent (Phase 7/8) | Multi-agent (Phase 11) |
|-|-------------------------|------------------------|
| Loop | One agent decides tools | Supervisor delegates specialists |
| Knowledge | Optional ad-hoc | Dedicated RAG agent |
| State | Tool observations | Structured specialist results |

### Example flow

Request: *Investigate MWS-1 and explain the issue, project context and relevant documentation.*

```
Supervisor
→ ISSUE_AGENT (MCP getIssue)
→ PROJECT_AGENT (MCP getProject/searchIssues)
→ KNOWLEDGE_AGENT (RAG)
→ Supervisor
→ FINALIZE
→ Final answer
```

API: `POST /api/v1/ai/multi-agent`  
UI: `/multi-agent`
