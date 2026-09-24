package com.avadhoot.workforgeai.ai.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgentRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message,

        @Min(value = 1, message = "maxSteps must be at least 1")
        @Max(value = 10, message = "maxSteps must be at most 10")
        Integer maxSteps,

        @Size(max = 64) String conversationId,

        @Size(max = 128) String sessionId
) {
    public AgentRequest(String message, Integer maxSteps) {
        this(message, maxSteps, null, null);
    }
}
