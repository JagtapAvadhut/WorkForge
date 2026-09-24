package com.avadhoot.workforgeai.ai.evaluation;

import java.time.Instant;
import java.util.List;

public record EvaluationRunSnapshot(
        String runId,
        String suite,
        String status,
        int totalCases,
        int completedCount,
        int passedCount,
        int failedCount,
        double passRate,
        long averageLatencyMs,
        long elapsedMs,
        String currentCaseId,
        String currentCategory,
        String error,
        Instant startedAt,
        Instant completedAt,
        List<EvaluationCaseResult> results
) {
}
