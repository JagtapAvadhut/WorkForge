# WorkForge AI — Memory (Phase 10)

Practical AI memory on PostgreSQL (`workforge_ai`). No Redis. No embedding-based memory search yet.

## 1. What AI memory is

Memory is **application-managed context** the backend loads and injects into the model prompt. The LLM does not “remember” by itself across requests — WorkForge AI retrieves stored rows and sends them as bounded context.

Three layers are kept separate:

| Layer | What it is | Where it lives |
|-------|------------|----------------|
| Conversation history | User/assistant turns | `ai_conversations` + `ai_messages` |
| Working / agent state | Tool steps, graph nodes, thoughts | In-request / graph state only |
| Long-term memory | Explicit preferences/facts | `ai_memories` |

## 2. Short-term memory

Recent messages for one conversation, capped by:

```yaml
workforge.ai.memory.max-recent-messages: 10
```

Loaded before each chat/agent/graph/mcp-agent turn. Older turns stay in the DB but are not resent unbounded.

## 3. Long-term memory

Explicit facts/preferences stored via `POST /api/v1/ai/memory/remember`. Never auto-promoted from every chat turn.

Example: `"User prefers simple explanations"` with category `preference` and importance `5`.

## 4. Working memory

Agent and LangGraph **working state** (steps, tool calls, intermediate nodes) lives only for the current request. It is returned in the API response for transparency but **not** written to `ai_memories` or as hidden chain-of-thought rows.

## 5. Conversation history

Durable user/assistant messages keyed by `conversation_id` and scoped by `session_id`.

Flow for chat / agent / graph-agent / mcp-agent:

1. Resolve or create conversation  
2. Load recent messages + relevant long-term memories  
3. Run model/agent with bounded context  
4. Save user + assistant messages  
5. Return `conversationId`

## 6. Memory retrieval

Before generation:

1. Recent conversation history (limit N)  
2. Relevant long-term memories for the session (filter by session, importance, recency — **not** vector search)  
3. Inject only that bounded block into the prompt  

## 7. Memory persistence

| Persist | Do not persist |
|---------|----------------|
| Conversation messages | Every agent internal step |
| Explicit long-term memories | Hidden chain-of-thought |
| Useful final interaction metadata (via messages) | Secrets / credentials |

Flyway: `V3__ai_memory.sql` → `ai_conversations`, `ai_messages`, `ai_memories`.

## 8. Memory vs RAG

| | Memory | RAG |
|-|--------|-----|
| Purpose | User/session-specific context | External knowledge / documents |
| Example | “User prefers simple English.” | “Sprint status values are FUTURE, ACTIVE, COMPLETE.” |
| Store | `ai_memories` / messages | Document chunks + PGVector |
| Retrieval | Structured filters | Embedding similarity |

Do not mix the two concepts in prompts or APIs.

## 9. Why not store every message as long-term memory

Chat turns are noisy, redundant, and often temporary. Treating them all as durable preferences pollutes future prompts, increases cost, and confuses the model. Long-term memory is **opt-in and explicit**.

## 10. Why agent internal reasoning should not be persisted

Decide/act/observe thoughts and graph node traces are working state. Persisting them as “memory” would leak intermediate reasoning, bloat storage, and often mislead later turns. Persist only final user/assistant messages (and explicit LTM).

---

## Request flows

### Chat

```
POST /api/v1/ai/chat
{ "conversationId": "...", "sessionId": "...", "message": "Explain it using a simple example." }
→ load history + LTM → Ollama → save turn → { response, conversationId }
```

### Agent / Graph / MCP agent

Same conversation prepare/complete around the existing Phase 7–9 loops. MCP protocol to `:8091` is unchanged; only the AI backend wraps the turn with memory.

### Long-term memory APIs

```
POST   /api/v1/ai/memory/conversations
GET    /api/v1/ai/memory/conversations/{id}
GET    /api/v1/ai/memory/conversations/{id}/messages
DELETE /api/v1/ai/memory/conversations/{id}

POST   /api/v1/ai/memory/remember
GET    /api/v1/ai/memory
GET    /api/v1/ai/memory/{id}
DELETE /api/v1/ai/memory/{id}
```

### Demo

1. Conversation: “What is RAG?” → later “Explain it using a simple example.” (history provides “it”)  
2. Remember preference → later “Explain embeddings.” (LTM shapes tone)  

UI: `/memory`, plus conversation selectors on `/ai-chat`, `/agent`, `/graph-agent`, `/mcp`.
