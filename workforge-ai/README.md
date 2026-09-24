# WorkForge AI

Separate AI learning playground through **Phase 12** (LLM → prompts → embeddings → PGVector → RAG → tools → agent → LangGraph4j → MCP → memory → multi-agent → evaluation / security / observability).

Does **not** modify the main WorkForge application or its database.

## Quick demo

1. Start AI backend `:8090`, MCP server `:8091`, Ollama, PostgreSQL (`workforge_ai`), AI frontend.
2. Seed demo knowledge:

```powershell
.\scripts\seed-ai-demo.ps1
```

3. Open the UI — every tab has **Samples** / **Load Demo** / suite buttons.
4. Smoke-test APIs:

```powershell
.\scripts\smoke-test-ai.ps1
```

Full walkthrough (every tab, sample inputs, expected results, evaluation suites): **[docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md)**.

## Ports

| Service | Port |
|---------|------|
| AI backend | 8090 |
| MCP server | 8091 |
| Main WorkForge (optional for tools) | 8080 |

## Docs

- [docs/README.md](docs/README.md) — index
- [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md) — demo + QA
- [docs/ai-roadmap.md](docs/ai-roadmap.md)
- Phase docs: embeddings, pgvector, rag, tool-calling, langgraph, mcp, memory, multi-agent, evaluation, security, observability
