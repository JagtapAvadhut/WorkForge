# RAG (Phase 5)

Retrieval-Augmented Generation on WorkForge AI: search PGVector, then answer with Ollama using only retrieved context.

## Difference: Chat vs RAG

| Mode | Flow |
|------|------|
| **Normal chat** (`POST /api/v1/ai/chat`) | Question → LLM → Answer |
| **RAG** (`POST /api/v1/ai/rag/query`) | Question → embed → PGVector search → Context → LLM → Answer + sources |

Chat stays unchanged. RAG is a separate capability.

## 1. What RAG is

RAG retrieves relevant knowledge **before** generation so the model answers from your documents instead of free-form memory.

## 2. Why LLM alone is insufficient

A chat model may invent plausible WorkForge facts. RAG grounds answers in stored embeddings (Phase 4 documents).

## 3. Retrieval

Question → `nomic-embed-text` → cosine search on `ai_documents` → top-K hits above `similarity-threshold`.

## 4. Augmentation

Hits are formatted into a CONTEXT block in the user prompt (`RagPromptBuilder`).

## 5. Generation

`ChatClient` uses a dedicated RAG system prompt + grounded user prompt. Sources are returned to the client.

## 6. Document context

Each source includes `id`, `content`, `metadata`, `similarity` — not the embedding vector.

## 7. Top-K

`topK` limits how many neighbors to retrieve (default 5, max `AI_RAG_MAX_TOP_K`).

## 8. Similarity threshold

`AI_RAG_SIMILARITY_THRESHOLD` (default **0.45**). Hits below the threshold are dropped. If none remain, the LLM is **not** called.

## 9. Grounded answers

System rules: use only CONTEXT; do not invent facts; do not claim DB access.

## 10. Hallucination / no-context behavior

Unknown questions return:

> I don't have enough information in the available knowledge.

with `sources: []`.

---

## Complete example

**Question:** `What is a sprint?`

1. **Embedding** — query vector (768-d)  
2. **Vector search** — nearest rows in `ai_documents`  
3. **Retrieved context** — sprint definition (similarity ~0.8+)  
4. **Final prompt** — RAG system + CONTEXT + QUESTION  
5. **LLM response** — answer based on that context + `sources[]`

## API

```http
POST /api/v1/ai/rag/query
{
  "question": "What is a sprint?",
  "topK": 5
}
```

```json
{
  "success": true,
  "data": {
    "answer": "...",
    "sources": [
      {
        "id": "...",
        "content": "...",
        "metadata": { "topic": "sprint" },
        "similarity": 0.79
      }
    ]
  }
}
```

## Seed knowledge (not app hardcoding)

```powershell
.\scripts\seed-rag-knowledge.ps1
```

Reads `data/rag-knowledge.json` and POSTs through document ingestion.

## Config

```yaml
workforge.ai.rag.default-top-k: 5
workforge.ai.rag.max-top-k: 20
workforge.ai.rag.similarity-threshold: 0.45
workforge.ai.rag.no-context-message: I don't have enough information...
```

UI: http://localhost:5174/rag
