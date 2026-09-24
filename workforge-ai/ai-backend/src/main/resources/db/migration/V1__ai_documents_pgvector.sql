-- Phase 4: pgvector + persistent document embeddings
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_documents (
    id          UUID PRIMARY KEY,
    content     TEXT NOT NULL,
    metadata    JSONB NOT NULL DEFAULT '{}'::jsonb,
    embedding   vector(${embeddingDimensions}) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS ai_documents_embedding_hnsw_idx
    ON ai_documents
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS ai_documents_created_at_idx
    ON ai_documents (created_at DESC);

CREATE OR REPLACE FUNCTION ai_documents_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ai_documents_updated_at ON ai_documents;
CREATE TRIGGER trg_ai_documents_updated_at
    BEFORE UPDATE ON ai_documents
    FOR EACH ROW
    EXECUTE FUNCTION ai_documents_set_updated_at();
