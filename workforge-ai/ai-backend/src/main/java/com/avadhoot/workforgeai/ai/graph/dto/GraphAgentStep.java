package com.avadhoot.workforgeai.ai.graph.dto;

import java.util.Map;

public record GraphAgentStep(
        String node,
        String action,
        Map<String, Object> details
) {
    public GraphAgentStep(String node, String action) {
        this(node, action, Map.of());
    }
}
