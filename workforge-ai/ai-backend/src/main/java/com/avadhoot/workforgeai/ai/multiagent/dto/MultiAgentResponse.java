package com.avadhoot.workforgeai.ai.multiagent.dto;

import java.util.List;
import java.util.Map;

public record MultiAgentResponse(
        String answer,
        List<String> agentsUsed,
        List<MultiAgentStep> steps,
        int iterations,
        int specialistCalls,
        String status,
        boolean completed,
        List<Map<String, Object>> specialistResults,
        List<Map<String, Object>> sources,
        List<Map<String, Object>> toolCalls,
        String conversationId
) {
}
