package com.avadhoot.workforgeai.ai.mcp.dto;

import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentStep;

import java.util.List;
import java.util.Map;

public record McpAgentResponse(
        String response,
        boolean completed,
        int iterations,
        String status,
        String currentNode,
        List<GraphAgentStep> steps,
        List<Map<String, Object>> toolsUsed,
        String conversationId
) {
    public McpAgentResponse(
            String response,
            boolean completed,
            int iterations,
            String status,
            String currentNode,
            List<GraphAgentStep> steps,
            List<Map<String, Object>> toolsUsed) {
        this(response, completed, iterations, status, currentNode, steps, toolsUsed, null);
    }
}
