package com.avadhoot.workforgeai.ai.observability;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TraceRepository {

    private final JdbcTemplate jdbcTemplate;

    public TraceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AiExecutionTrace insert(AiExecutionTrace trace) {
        jdbcTemplate.update(
                """
                INSERT INTO ai_execution_traces (
                    id, trace_id, feature, model, started_at, completed_at, latency_ms, success,
                    error_code, tool_call_count, agent_step_count, mcp_call_count, retrieval_count,
                    input_tokens, output_tokens, total_tokens, request_summary
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                trace.id(),
                trace.traceId(),
                trace.feature(),
                trace.model(),
                Timestamp.from(trace.startedAt()),
                trace.completedAt() == null ? null : Timestamp.from(trace.completedAt()),
                trace.latencyMs(),
                trace.success(),
                trace.errorCode(),
                trace.toolCallCount(),
                trace.agentStepCount(),
                trace.mcpCallCount(),
                trace.retrievalCount(),
                trace.inputTokens(),
                trace.outputTokens(),
                trace.totalTokens(),
                trace.requestSummary());
        return trace;
    }

    public Optional<AiExecutionTrace> findByTraceId(String traceId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    """
                    SELECT id, trace_id, feature, model, started_at, completed_at, latency_ms, success,
                           error_code, tool_call_count, agent_step_count, mcp_call_count, retrieval_count,
                           input_tokens, output_tokens, total_tokens, request_summary
                    FROM ai_execution_traces
                    WHERE trace_id = ?
                    """,
                    ROW_MAPPER,
                    traceId));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<AiExecutionTrace> list(int limit, int offset) {
        return jdbcTemplate.query(
                """
                SELECT id, trace_id, feature, model, started_at, completed_at, latency_ms, success,
                       error_code, tool_call_count, agent_step_count, mcp_call_count, retrieval_count,
                       input_tokens, output_tokens, total_tokens, request_summary
                FROM ai_execution_traces
                ORDER BY started_at DESC
                LIMIT ? OFFSET ?
                """,
                ROW_MAPPER,
                Math.max(1, Math.min(limit, 200)),
                Math.max(0, offset));
    }

    public int updateCompletion(
            String traceId,
            String model,
            Instant completedAt,
            long latencyMs,
            boolean success,
            String errorCode,
            int toolCallCount,
            int agentStepCount,
            int mcpCallCount,
            int retrievalCount,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens) {
        return jdbcTemplate.update(
                """
                UPDATE ai_execution_traces
                SET model = ?,
                    completed_at = ?,
                    latency_ms = ?,
                    success = ?,
                    error_code = ?,
                    tool_call_count = ?,
                    agent_step_count = ?,
                    mcp_call_count = ?,
                    retrieval_count = ?,
                    input_tokens = ?,
                    output_tokens = ?,
                    total_tokens = ?
                WHERE trace_id = ?
                """,
                model,
                Timestamp.from(completedAt),
                latencyMs,
                success,
                errorCode,
                toolCallCount,
                agentStepCount,
                mcpCallCount,
                retrievalCount,
                inputTokens,
                outputTokens,
                totalTokens,
                traceId);
    }

    public long count() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ai_execution_traces", Long.class);
        return value == null ? 0L : value;
    }

    public ObservabilitySummary summary() {
        return jdbcTemplate.query(
                """
                SELECT
                    COUNT(*) AS total_requests,
                    COALESCE(SUM(CASE WHEN success THEN 1 ELSE 0 END), 0) AS successful_requests,
                    COALESCE(SUM(CASE WHEN success THEN 0 ELSE 1 END), 0) AS failed_requests,
                    COALESCE(AVG(latency_ms), 0) AS average_latency,
                    COALESCE(SUM(tool_call_count), 0) AS tool_call_count,
                    COALESCE(SUM(CASE WHEN agent_step_count > 0 THEN 1 ELSE 0 END), 0) AS agent_executions,
                    COALESCE(SUM(mcp_call_count), 0) AS mcp_executions,
                    COALESCE(SUM(retrieval_count), 0) AS rag_retrieval_count
                FROM ai_execution_traces
                """,
                rs -> {
                    rs.next();
                    return new ObservabilitySummary(
                            rs.getLong("total_requests"),
                            rs.getLong("successful_requests"),
                            rs.getLong("failed_requests"),
                            rs.getDouble("average_latency"),
                            rs.getLong("tool_call_count"),
                            rs.getLong("agent_executions"),
                            rs.getLong("mcp_executions"),
                            rs.getLong("rag_retrieval_count"));
                });
    }

    private static final RowMapper<AiExecutionTrace> ROW_MAPPER = (rs, rowNum) -> new AiExecutionTrace(
            UUID.fromString(rs.getString("id")),
            rs.getString("trace_id"),
            rs.getString("feature"),
            rs.getString("model"),
            toInstant(rs.getTimestamp("started_at")),
            toInstant(rs.getTimestamp("completed_at")),
            rs.getObject("latency_ms") == null ? null : rs.getLong("latency_ms"),
            rs.getBoolean("success"),
            rs.getString("error_code"),
            rs.getInt("tool_call_count"),
            rs.getInt("agent_step_count"),
            rs.getInt("mcp_call_count"),
            rs.getInt("retrieval_count"),
            (Integer) rs.getObject("input_tokens"),
            (Integer) rs.getObject("output_tokens"),
            (Integer) rs.getObject("total_tokens"),
            rs.getString("request_summary"));

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
