package com.avadhoot.workforgeai.ai.evaluation;

import java.util.List;

public record EvaluationCase(
        String id,
        String category,
        String input,
        String expected,
        List<String> expectedTools,
        List<String> expectedAgents,
        String checkType,
        long timeoutMs,
        List<String> suites
) {
    public EvaluationCase {
        expectedTools = expectedTools == null ? List.of() : List.copyOf(expectedTools);
        expectedAgents = expectedAgents == null ? List.of() : List.copyOf(expectedAgents);
        suites = suites == null || suites.isEmpty() ? List.of("ALL") : List.copyOf(suites);
        if (timeoutMs <= 0) {
            timeoutMs = 60_000L;
        }
    }

    public boolean inSuite(String suite) {
        String want = suite == null ? "ALL" : suite.trim().toUpperCase();
        if ("ALL".equals(want)) {
            return true;
        }
        return suites.stream().anyMatch(s -> s.equalsIgnoreCase(want));
    }
}
