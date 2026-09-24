package com.avadhoot.workforgeai.ai.evaluation;

import java.util.List;

public record EvaluationRunResult(
        int totalCases,
        int passed,
        int failed,
        double passRate,
        long averageLatencyMs,
        List<EvaluationCaseResult> results
) {
}
