package com.avadhoot.workforgeai.ai.graph.dto;

import java.util.List;
import java.util.Map;

public record GraphAgentResponse(
        String response,
        boolean completed,
        int iterations,
        String status,
        String currentNode,
        List<GraphAgentStep> steps,
        List<Map<String, Object>> toolCalls,
        List<Map<String, Object>> toolResults,
        String conversationId
) {
    public GraphAgentResponse(
            String response,
            boolean completed,
            int iterations,
            String status,
            String currentNode,
            List<GraphAgentStep> steps,
            List<Map<String, Object>> toolCalls,
            List<Map<String, Object>> toolResults) {
        this(response, completed, iterations, status, currentNode, steps, toolCalls, toolResults, null);
    }
}
