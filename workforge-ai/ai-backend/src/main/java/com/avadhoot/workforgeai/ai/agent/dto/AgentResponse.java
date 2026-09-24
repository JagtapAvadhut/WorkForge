package com.avadhoot.workforgeai.ai.agent.dto;

import com.avadhoot.workforgeai.ai.agent.AgentStep;

import java.util.List;

public record AgentResponse(
        String answer,
        List<AgentStep> steps,
        String stopReason,
        int iterations,
        String conversationId
) {
    public AgentResponse(String answer, List<AgentStep> steps, String stopReason, int iterations) {
        this(answer, steps, stopReason, iterations, null);
    }
}
