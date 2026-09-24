# Tool Calling (Phase 6)

How WorkForge AI lets the LLM request Java tools that query a controlled sample dataset.

## 1. What tool calling is

The model can ask the application to run a named function with JSON arguments. Spring AI executes that Java method and feeds the result back to the model.

## 2. Why an LLM needs tools

Models do not have live access to your database. Tools bridge language → structured application actions.

## 3–5. Tool definition / description / parameter schema

Tools are declared with `@Tool` / `@ToolParam` on `WorkforgeTools`:

| Tool | Purpose |
|------|---------|
| `getIssue` | One issue by exact key (`MWS-1`) |
| `searchIssues` | Filtered list (`projectKey`, `status`, `assignee`, `limit`) |
| `getProject` | Project by key (`MWS`) |

Inspect what the model sees:

```http
POST /api/v1/ai/tool-preview
{ "message": "Find open MWS issues" }
```

Returns name, description, and JSON `inputSchema` — **no execution**.

## 6. LLM tool selection

`POST /api/v1/ai/tool-chat` sends the user message plus tool definitions. The model decides whether to call tools.

## 7. Who executes Java

**Spring AI / the JVM** executes `@Tool` methods. The LLM never runs Java or SQL.

Flow: Tool → Service → Repository → PostgreSQL (`workforge_ai` sample tables).

## 8. Tool result flow

1. Model emits a tool call  
2. `RecordingToolCallback` runs the Java method and records args/result  
3. Result returns to the model  
4. Model writes the final natural-language answer  

## 9. Normal vs tool response

| Question | Expected |
|----------|----------|
| "What is a sprint?" | Answer, `toolCalls: []` |
| "Show open MWS issues" | `searchIssues` then answer |

## 10. Multiple tool calls

"Tell me about project MWS and its open issues" may call `getProject` + `searchIssues` in one Spring AI tool-calling turn (not an agent loop).

## 11. Read-only safety

Only read tools exist. No create/update/delete. Limits capped (`AI_TOOLS_MAX_SEARCH_LIMIT`).

---

## Complete example

**User:** `Show open MWS issues`

1. LLM selects `searchIssues(projectKey=MWS, status=OPEN, …)`  
2. Spring AI invokes Java `WorkforgeTools.searchIssues`  
3. `IssueToolService` → repository → `wf_issues`  
4. JSON issue list returned to the model  
5. LLM answers with those issues  

## API

```http
POST /api/v1/ai/tool-chat
{ "message": "Show me open MWS issues assigned to Avadhoot" }
```

```json
{
  "success": true,
  "data": {
    "response": "...",
    "toolCalls": [
      {
        "tool": "searchIssues",
        "arguments": { "projectKey": "MWS", "status": "OPEN", "assignee": "avadhoot" },
        "resultSummary": "{\"count\":2,...}"
      }
    ]
  }
}
```

Existing `/api/v1/ai/chat` and `/api/v1/ai/rag/query` are unchanged.

UI: http://localhost:5174/tools

## Note on local models

Some local Ollama models print tool calls as JSON text instead of native `tool_calls`.
WorkForge AI detects that pattern, **executes the Java tool anyway**, then asks the model for a final answer.
The LLM still never talks to PostgreSQL directly.
