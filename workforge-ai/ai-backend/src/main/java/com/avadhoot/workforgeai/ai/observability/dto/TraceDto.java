package com.avadhoot.workforgeai.ai.observability.dto;

import java.time.Instant;
import java.util.UUID;

public record TraceDto(
        UUID id,
        String traceId,
        String feature,
        String model,
        Instant startedAt,
        Instant completedAt,
        Long latencyMs,
        boolean success,
        String errorCode,
        int toolCallCount,
        int agentStepCount,
        int mcpCallCount,
        int retrievalCount,
        Integer inputTokens,
        Integer outputTokens,
        Integer totalTokens,
        String requestSummary
) {
}
