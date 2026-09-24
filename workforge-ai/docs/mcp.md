# MCP (Phase 9)

Model Context Protocol for WorkForge AI — a **real protocol boundary** between the AI backend and tools.

## 1. What MCP is

MCP is an open protocol for exposing tools/resources to AI clients over a standard transport.
Clients discover tools dynamically and invoke them without embedding the tool implementations.

## 2. Why MCP exists

Phase 6 wires tools as local Java `@Tool` methods in the same JVM.
MCP separates **who decides** (LLM/agent) from **where tools live** (remote MCP server), so tools can be shared across apps/agents with allowlisting and process isolation.

## 3–4. MCP Client / Server

| Process | Port | Role |
|---------|------|------|
| AI Backend | **8090** | MCP **client** + LLM / LangGraph |
| MCP Server | **8091** | Streamable-HTTP MCP **server** + read-only tools |

Dependencies (Spring AI **2.0.0**):

- Server: `spring-ai-starter-mcp-server-webmvc`
- Client: `spring-ai-starter-mcp-client`
- Annotations: `@McpTool` / `@McpToolParam` (`spring-ai-mcp-annotations`)

## 5. Tool discovery

`GET /api/v1/ai/mcp/tools` lists tools actually discovered from the MCP server via `ToolCallbackProvider` (allowlisted).

## 6. Tool execution

```
LLM / Graph node
  → MCP Client (8090)
  → Streamable HTTP (/mcp)
  → MCP Server (8091)
  → @McpTool method
  → IssueToolService / ProjectToolService
  → Repository
  → PostgreSQL (workforge_ai)
```

Business logic lives in shared module `workforge-ai-sample-domain` — not duplicated, not called as local Phase 6 shortcuts from MCP mode.

## 7–8. Transport / Streamable HTTP

```yaml
# mcp-server
spring.ai.mcp.server.protocol: STREAMABLE
spring.ai.mcp.server.streamable-http.mcp-endpoint: /mcp

# ai-backend client
spring.ai.mcp.client.streamable-http.connections.workforge-mcp.url: http://localhost:8091
spring.ai.mcp.client.streamable-http.connections.workforge-mcp.endpoint: /mcp
spring.ai.mcp.client.initialized: false   # soft-start if server is down
```

Endpoint: `http://localhost:8091/mcp`

## 9. MCP vs Phase 6 tool calling

| | Phase 6 `/tool-chat` | Phase 9 `/mcp-chat` |
|--|----------------------|---------------------|
| Path | LLM → local Java `@Tool` | LLM → MCP Client → protocol → MCP Server → tool |
| Process | Same JVM | Separate MCP server process |
| Discovery | Compile-time beans | Runtime MCP discovery |

## 10. MCP vs LangGraph (Phase 8)

| | Phase 8 `/graph-agent` | Phase 9 `/mcp-agent` |
|--|------------------------|----------------------|
| Orchestration | LangGraph4j nodes/edges | Same graph shape |
| Tool execution | Local `WorkforgeTools` | MCP `McpExecuteToolNode` |

LangGraph orchestrates workflow/state. MCP transports tool calls. Neither “makes the model smarter.”

## 11. Security / allowlisting

Exposed tools (READ-ONLY only):

- `getIssue`
- `searchIssues`
- `getProject`

Configured allowlist: `workforge.ai.mcp.allowed-tools`.
No delete/create/update/email/payment/filesystem/shell tools.

## Architecture

```
React → AI Backend :8090
              ↓
         MCP Client
              ↓
         MCP Server :8091
              ↓
    getIssue / searchIssues / getProject
              ↓
         PostgreSQL
```

## Example: “Get MWS-1”

1. `POST /api/v1/ai/mcp-chat` `{ "message": "Get MWS-1" }`
2. Backend attaches MCP-discovered tools to ChatClient
3. LLM selects `getIssue`
4. MCP client invokes tool over Streamable HTTP
5. MCP server runs `@McpTool getIssue` → `IssueToolService`
6. Result returns to LLM → final answer

## APIs

| Method | Path |
|--------|------|
| GET | `/api/v1/ai/mcp/status` |
| GET | `/api/v1/ai/mcp/tools` |
| POST | `/api/v1/ai/mcp-chat` |
| POST | `/api/v1/ai/mcp-agent` |

UI: http://localhost:5174/mcp

## Run

```bash
# Terminal 1 — MCP server
cd workforge-ai
.\mvnw.cmd -pl mcp-server -am spring-boot:run

# Terminal 2 — AI backend
.\mvnw.cmd -pl ai-backend -am spring-boot:run
```
