package com.avadhoot.workforgeai.ai.multiagent;

import java.util.List;
import java.util.Map;

/**
 * Structured specialist contract consumed by the Supervisor.
 */
public record SpecialistResult(
        String agent,
        String status,
        String summary,
        List<String> findings,
        List<Map<String, Object>> sources,
        List<Map<String, Object>> toolCalls
) {
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILURE = "FAILURE";
    public static final String SKIPPED = "SKIPPED";

    public SpecialistResult {
        findings = findings == null ? List.of() : List.copyOf(findings);
        sources = sources == null ? List.of() : List.copyOf(sources);
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public static SpecialistResult success(
            String agent,
            String summary,
            List<String> findings,
            List<Map<String, Object>> sources,
            List<Map<String, Object>> toolCalls) {
        return new SpecialistResult(agent, SUCCESS, summary, findings, sources, toolCalls);
    }

    public static SpecialistResult failure(String agent, String summary) {
        return new SpecialistResult(agent, FAILURE, summary, List.of(), List.of(), List.of());
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "agent", agent == null ? "" : agent,
                "status", status == null ? "" : status,
                "summary", summary == null ? "" : summary,
                "findings", findings,
                "sources", sources,
                "toolCalls", toolCalls);
    }

    @SuppressWarnings("unchecked")
    public static SpecialistResult fromMap(Map<String, Object> map) {
        if (map == null) {
            return failure("UNKNOWN", "empty result");
        }
        Object findingsObj = map.get("findings");
        Object sourcesObj = map.get("sources");
        Object toolsObj = map.get("toolCalls");
        List<String> findings = findingsObj instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        List<Map<String, Object>> sources = sourcesObj instanceof List<?> list
                ? list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList()
                : List.of();
        List<Map<String, Object>> toolCalls = toolsObj instanceof List<?> list
                ? list.stream()
                        .filter(Map.class::isInstance)
                        .map(item -> (Map<String, Object>) item)
                        .toList()
                : List.of();
        return new SpecialistResult(
                String.valueOf(map.getOrDefault("agent", "")),
                String.valueOf(map.getOrDefault("status", FAILURE)),
                String.valueOf(map.getOrDefault("summary", "")),
                findings,
                sources,
                toolCalls);
    }
}
