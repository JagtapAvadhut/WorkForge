# PGVector (Phase 4)

How WorkForge AI persists embeddings in PostgreSQL and runs real semantic search.

**Database:** `workforge_ai` on local PostgreSQL (separate from the main WorkForge app DB)  
**Extension:** `vector` (pgvector)  
**Table:** `ai_documents`  
**Embedding model:** `nomic-embed-text` → **768** dimensions (`AI_EMBEDDING_DIMENSIONS`)  
**Store:** Spring AI `PgVectorStore` + Flyway-managed schema

## Setup

```bash
# Ensure DB exists (once)
# CREATE DATABASE workforge_ai;

cd ai-backend
# env (defaults shown):
# DB_HOST=localhost DB_PORT=5432 DB_NAME=workforge_ai
# DB_USERNAME=postgres DB_PASSWORD=postgres
# AI_EMBEDDING_DIMENSIONS=768
# OLLAMA_EMBEDDING_MODEL=nomic-embed-text

.\mvnw.cmd spring-boot:run
```

Flyway runs `V1__ai_documents_pgvector.sql` on startup.

Optional Docker Postgres with pgvector: see `docker/docker-compose.yml` (maps host `5433`).

## 1. What vector storage solves

In-memory cosine compare (Phase 3) is great for learning, but production needs **durable** vectors so every search does not re-embed your whole corpus.

## 2. Why vectors need persistence

Documents are ingested once → embedding written to Postgres → later queries only embed the **query** and search stored vectors.

## 3. PostgreSQL + pgvector

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

`embedding vector(768)` stores the float array natively. HNSW index on cosine ops accelerates similarity.

## 4. Embedding storage

```http
POST /api/v1/ai/documents
{
  "content": "A sprint is a fixed development period used by a team.",
  "metadata": { "type": "knowledge", "topic": "sprint" }
}
```

Flow: content → Ollama embedding model → Spring AI `VectorStore.add` → row in `ai_documents`.

Response includes `id`, `content`, `metadata`, `embeddingDimensions`, `createdAt`.

## 5. Similarity search

```http
POST /api/v1/ai/documents/search
{
  "query": "How does a sprint work?",
  "topK": 5
}
```

Query is embedded, then PGVector returns nearest neighbors (cosine distance → similarity score).

Example (local validation): query `"How does a sprint work?"` ranked the sprint document first (~0.79), ahead of epic/backlog/workflow/bug.

## 6. topK

`topK` caps how many nearest documents to return (default 5, max configured by `AI_DOCUMENTS_MAX_TOP_K`).

Validation rejects blank query and `topK <= 0` / over max.

## 7. Metadata

JSONB column for filters/labels (`type`, `topic`, …). Stored with the document; returned in list/search.

## 8. SQL search vs vector search

| Approach | Matches |
|----------|---------|
| `LIKE` / full-text | Shared keywords |
| Vector search | Shared **meaning** (paraphrases) |

`"How does a sprint work?"` can rank a doc about "fixed development period" highly even without the word "how".

## 9. Why PGVector before RAG

RAG needs retrieve → prompt → generate. Retrieval needs a vector index of knowledge. Phase 4 builds that index; Phase 5 will feed top hits into the LLM prompt.

---

## Other APIs

| Method | Path |
|--------|------|
| GET | `/api/v1/ai/documents?page=0&size=20` |
| DELETE | `/api/v1/ai/documents/{id}` |

UI: http://localhost:5174/documents
