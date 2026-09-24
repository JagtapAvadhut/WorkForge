# WorkForge AI

Independent AI learning playground for WorkForge concepts.  
**Does not modify** the main WorkForge application under `D:\IVL\backend` / `frontend`.

## 1. Architecture

```
ai-frontend (React)  --HTTP-->  ai-backend (Spring Boot + Spring AI)
                                      |
                    +-----------------+------------------+
                    v                                    v
             Ollama (chat + embed)              PostgreSQL + pgvector
```

- Package: `com.avadhoot.workforgeai`
- Phases 1–12 complete (through evaluation, security, observability)
- Separate DB `workforge_ai` — does not modify the main WorkForge schema
- Demo guide: [DEMO_GUIDE.md](DEMO_GUIDE.md)

## 2. Setup

Prerequisites:

- JDK 21
- Node.js 20+
- Ollama installed and running
- Chat model: `qwen2.5-coder:3b` (`OLLAMA_MODEL`)
- Embedding model: `nomic-embed-text` (`OLLAMA_EMBEDDING_MODEL`)
- PostgreSQL with pgvector (`DB_NAME=workforge_ai`)

```bash
cd D:\IVL\workforge-ai
```

## 3. Ollama setup

```bash
# Install from https://ollama.com if needed, then:
ollama serve          # if not already running as a service
ollama pull qwen2.5-coder:3b
ollama pull nomic-embed-text
ollama list
```

Default API: `http://localhost:11434`

## 4. Backend run

```bash
cd ai-backend
# optional:
# set OLLAMA_BASE_URL=http://localhost:11434
# set OLLAMA_MODEL=qwen2.5-coder:3b
# set SERVER_PORT=8090
# set AI_MAX_HISTORY_MESSAGES=10

.\mvnw.cmd spring-boot:run
```

Health: http://localhost:8090/actuator/health

## 5. Frontend run

```bash
cd ai-frontend
npm install
npm run dev
```

UI: http://localhost:5174/ai-chat

Prefer the Vite proxy (`VITE_API_BASE_URL=/api/v1`) to avoid CORS. Direct calls to `:8090` are also allowed from `localhost` / `127.0.0.1`.

## 6. API example

```bash
curl -X POST http://localhost:8090/api/v1/ai/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Explain sprint\",\"strategy\":\"DOMAIN_EXPERT\"}"
```

```json
{
  "success": true,
  "data": {
    "response": "...",
    "strategy": "DOMAIN_EXPERT"
  }
}
```

Prompt preview (learning / developer):

```bash
curl -X POST http://localhost:8090/api/v1/ai/prompt-preview ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Explain sprint\",\"strategy\":\"FEW_SHOT\"}"
```

## 7. Current AI capability (Phase 12)

- Chat, embeddings, PGVector, RAG, tool calling, agent, LangGraph, MCP, memory, multi-agent
- Evaluation suites (SMOKE / CORE / ALL) with async runs + progress polling
- Security heuristics + observability traces
- Demo seed + smoke scripts under `scripts/`

Details: [DEMO_GUIDE.md](DEMO_GUIDE.md) · [evaluation.md](evaluation.md) · [ai-roadmap.md](ai-roadmap.md)

## 8. Next steps

Learning track **Phases 1–12 complete**. Use the demo guide for QA and demos.