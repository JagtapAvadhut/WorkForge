# Embeddings (Phase 3)

How WorkForge AI turns text into vectors and ranks meaning with cosine similarity.

**Embedding model (separate from chat):** `nomic-embed-text` via `OLLAMA_EMBEDDING_MODEL`  
**Chat model (unchanged):** `qwen2.5-coder:3b` via `OLLAMA_MODEL`

## Setup

```bash
ollama pull nomic-embed-text
ollama list   # confirm nomic-embed-text is present
```

```yaml
# ai-backend application.yml
spring.ai.ollama.embedding.model: ${OLLAMA_EMBEDDING_MODEL:nomic-embed-text}
spring.ai.ollama.chat.options.model: ${OLLAMA_MODEL:qwen2.5-coder:3b}
```

## 1. What an embedding is

An embedding is a numeric fingerprint of text. The model maps a sentence into a fixed-length list of floats so that **similar meanings land near each other** in vector space.

Implemented in `EmbeddingService.embed(String)` using Spring AI `EmbeddingModel`.

## 2. Why embeddings are vectors

Vectors let us measure distance/angle with math (dot product, norms). Words alone cannot do that reliably.

**API**

```http
POST /api/v1/ai/embeddings
{ "text": "What is a sprint?" }
```

**Example response (local `nomic-embed-text`)**

```json
{
  "success": true,
  "data": {
    "text": "What is a sprint?",
    "dimensions": 768,
    "embedding": [-0.0240, 0.0388, -0.1936]
  }
}
```

(Full vector has 768 floats; UI/docs show a short preview.)

`dimensions` is taken from the returned vector length — not hardcoded.

## 3. Why normal text equality is insufficient

`"How do I create a sprint?"` ≠ `"How can I start a new sprint?"` as strings, but they mean nearly the same thing. Equality fails; embeddings succeed.

## 4. Cosine similarity

```
similarity = (a · b) / (||a|| × ||b||)
```

Implemented in `EmbeddingSimilarityService.cosineSimilarity`.

Guards:

- null / empty vectors
- zero vectors (norm == 0)
- dimension mismatch

```http
POST /api/v1/ai/embeddings/similarity
{
  "text1": "How do I create a sprint?",
  "text2": "How can I start a new sprint?"
}
```

```json
{ "success": true, "data": { "similarity": 0.85 } }
```

Paraphrases about creating a sprint score ~0.85; a sprint question vs a bug definition scores ~0.37 on the same model.

## 5. Dimensions

`nomic-embed-text` produces **768** floats per input in this environment. Always read `data.dimensions` from the API.

## 6. Query embedding

The user question is embedded once — that vector is the search key.

## 7. Document embedding

Each candidate document is embedded (batch when possible via `EmbeddingModel.embed(List)`).

```http
POST /api/v1/ai/embeddings/batch
{
  "texts": [
    "What is a sprint?",
    "What is a backlog?",
    "How do I create a bug?"
  ]
}
```

## 8. Semantic similarity / ranking

```http
POST /api/v1/ai/embeddings/compare
{
  "query": "How do I create a sprint?",
  "documents": [
    "A sprint is a fixed development period.",
    "A backlog contains planned work.",
    "A bug describes incorrect system behavior."
  ]
}
```

Returns documents sorted by cosine similarity descending — the first practical step toward vector search.

## 9. Why embeddings are required for RAG

RAG retrieves relevant chunks **before** the LLM answers. Retrieval needs a similarity signal over meaning, not keyword match alone. Embeddings provide that signal; Phase 4 stores them in PGVector; Phase 5 wires retrieve → prompt → generate.

---

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/v1/ai/embeddings` | Single embedding |
| POST | `/api/v1/ai/embeddings/batch` | Batch embeddings |
| POST | `/api/v1/ai/embeddings/similarity` | Cosine between two texts |
| POST | `/api/v1/ai/embeddings/compare` | Rank documents vs query |

UI: http://localhost:5174/embeddings
