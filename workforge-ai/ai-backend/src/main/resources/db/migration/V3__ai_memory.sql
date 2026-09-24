-- Phase 10: conversation history + explicit long-term memory
CREATE TABLE IF NOT EXISTS ai_conversations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   VARCHAR(128) NOT NULL,
    title        VARCHAR(300),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_conversations_session_updated
    ON ai_conversations (session_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role             VARCHAR(32) NOT NULL,
    content          TEXT NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ai_messages_role CHECK (role IN ('user', 'assistant', 'system'))
);

CREATE INDEX IF NOT EXISTS idx_ai_messages_conversation_created
    ON ai_messages (conversation_id, created_at ASC);

CREATE TABLE IF NOT EXISTS ai_memories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   VARCHAR(128) NOT NULL,
    category     VARCHAR(64) NOT NULL,
    content      TEXT NOT NULL,
    source       VARCHAR(64) NOT NULL DEFAULT 'explicit',
    importance   INT NOT NULL DEFAULT 3,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ai_memories_importance CHECK (importance BETWEEN 1 AND 10),
    CONSTRAINT chk_ai_memories_category CHECK (char_length(trim(category)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_ai_memories_session_active_importance
    ON ai_memories (session_id, active, importance DESC, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_memories_session_category
    ON ai_memories (session_id, category)
    WHERE active = TRUE;
