# Observability (Phase 12)

## Tracing

Each instrumented AI request creates an `ai_execution_traces` row:

- `traceId`, feature, model, latency, success/errorCode
- counts: tool / agent step / MCP / RAG retrieval
- sanitized `requestSummary` (truncated, secrets redacted)
- token fields nullable (Ollama often does not expose usage)

Flow:

```
Request → Security Check → Trace Start → AI Feature → Trace Complete → Response
```

Instrumented features: CHAT, RAG, TOOL, AGENT, GRAPH, MCP_CHAT, MCP_AGENT, MULTI_AGENT.

## Metrics

`GET /api/v1/ai/observability/summary` aggregates totals, success/failure, average latency, tool/MCP/RAG counts.

Traces: paginated `GET /api/v1/ai/observability/traces` and detail by `traceId`.

## Token / cost metadata

If Spring AI/Ollama provides usage metadata, store input/output/total tokens.  
Otherwise store `null` — **never invent** counts.

`TokenCostCalculator` is pluggable; default `NullTokenCostCalculator` returns null cost.

## Privacy

Do **not** persist passwords, JWTs, refresh tokens, secrets, or chain-of-thought.  
Summaries only.

UI: `/observability`
