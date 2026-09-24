-- Phase 12: AI execution observability (summaries only, no secrets / CoT)
CREATE TABLE IF NOT EXISTS ai_execution_traces (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id          VARCHAR(64) NOT NULL,
    feature           VARCHAR(64) NOT NULL,
    model             VARCHAR(128),
    started_at        TIMESTAMPTZ NOT NULL,
    completed_at      TIMESTAMPTZ,
    latency_ms        BIGINT,
    success           BOOLEAN NOT NULL DEFAULT FALSE,
    error_code        VARCHAR(64),
    tool_call_count   INT NOT NULL DEFAULT 0,
    agent_step_count  INT NOT NULL DEFAULT 0,
    mcp_call_count    INT NOT NULL DEFAULT 0,
    retrieval_count   INT NOT NULL DEFAULT 0,
    input_tokens      INT,
    output_tokens     INT,
    total_tokens      INT,
    request_summary   VARCHAR(500),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_ai_execution_traces_trace_id UNIQUE (trace_id)
);

CREATE INDEX IF NOT EXISTS idx_ai_execution_traces_started
    ON ai_execution_traces (started_at DESC);

CREATE INDEX IF NOT EXISTS idx_ai_execution_traces_feature_started
    ON ai_execution_traces (feature, started_at DESC);
