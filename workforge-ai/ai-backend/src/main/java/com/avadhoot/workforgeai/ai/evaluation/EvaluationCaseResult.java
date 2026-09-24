package com.avadhoot.workforgeai.ai.evaluation;

import java.util.List;

public record EvaluationCaseResult(
        String caseId,
        String category,
        boolean passed,
        String expected,
        String actualAnswer,
        List<String> expectedTools,
        List<String> actualTools,
        List<String> expectedAgents,
        List<String> actualAgents,
        long latencyMs,
        String error,
        String traceId
) {
    public EvaluationCaseResult {
        expectedTools = expectedTools == null ? List.of() : List.copyOf(expectedTools);
        actualTools = actualTools == null ? List.of() : List.copyOf(actualTools);
        expectedAgents = expectedAgents == null ? List.of() : List.copyOf(expectedAgents);
        actualAgents = actualAgents == null ? List.of() : List.copyOf(actualAgents);
    }
}
