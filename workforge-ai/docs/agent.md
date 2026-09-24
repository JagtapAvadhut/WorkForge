# AI Agent (Phase 7)

Controlled agent loop over Phase 6 read-only tools.

## Difference: Tool Chat vs Agent vs Graph Agent

| Mode | Behavior |
|------|----------|
| **Tool chat** (`/api/v1/ai/tool-chat`) | One model turn may call tools; Spring AI handles the exchange |
| **Agent** (`/api/v1/ai/agent/run`) | Explicit Java loop: decide → tool → observe → decide again → final |
| **Graph agent** (`/api/v1/ai/graph-agent`) | Same idea as Phase 7, but orchestration is a LangGraph4j `StateGraph` (nodes/edges/state) |

See [langgraph.md](./langgraph.md) for Phase 8 details. LangGraph does **not** make the LLM smarter — it structures workflow/state around the agent.

## Loop

```
User request
 → AgentService
 → decide (JSON action)
 → if tool: execute WorkforgeTools via service/repository
 → observe result
 → repeat until final or maxSteps
 → answer
```

## Components

- `AgentService` — orchestration only
- `AgentState` / `AgentStep` / `AgentResult` — minimal state
- Reuses Phase 6 `WorkforgeTools` (`getIssue`, `searchIssues`, `getProject`)

## Decision format

```json
{"action":"tool","thought":"...","tool":"getProject","arguments":{"projectKey":"MWS"}}
{"action":"final","thought":"...","answer":"..."}
```

## API

```http
POST /api/v1/ai/agent/run
{
  "message": "Tell me about project MWS and its open issues",
  "maxSteps": 5
}
```

Response includes `answer`, `steps[]` (thought/action/tool/arguments/observation), `stopReason`, `iterations`.

`stopReason`: `completed` | `max_steps` | `invalid_decision`

## Safety

- Read-only tools only
- Max steps capped (`AI_AGENT_MAX_STEPS`, request max 10)
- Unknown tools recorded and loop continues
- No LangGraph / MCP / multi-agent yet

## Config

```yaml
workforge.ai.agent.max-steps: 5
```

UI: http://localhost:5174/agent
