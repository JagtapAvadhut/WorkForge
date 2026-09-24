package com.avadhoot.workforgeai.ai.observability;

public record ObservabilitySummary(
        long totalRequests,
        long successfulRequests,
        long failedRequests,
        double averageLatencyMs,
        long toolCallCount,
        long agentExecutions,
        long mcpExecutions,
        long ragRetrievalCount
) {
}
