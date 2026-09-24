package com.avadhoot.workforgeai.ai.multiagent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MultiAgentRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message,

        @Size(max = 64) String conversationId,

        @Size(max = 128) String sessionId,

        @Min(1) @Max(5) Integer maxIterations,

        @Min(1) @Max(20) Integer maxSpecialistCalls
) {
    public MultiAgentRequest(String message) {
        this(message, null, null, null, null);
    }
}
