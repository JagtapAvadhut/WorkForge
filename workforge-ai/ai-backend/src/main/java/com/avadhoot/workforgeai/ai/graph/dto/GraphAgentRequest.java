package com.avadhoot.workforgeai.ai.graph.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GraphAgentRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message,

        @Min(value = 1, message = "maxIterations must be at least 1")
        @Max(value = 10, message = "maxIterations must be at most 10")
        Integer maxIterations,

        @Size(max = 64) String conversationId,

        @Size(max = 128) String sessionId
) {
    public GraphAgentRequest(String message, Integer maxIterations) {
        this(message, maxIterations, null, null);
    }
}
