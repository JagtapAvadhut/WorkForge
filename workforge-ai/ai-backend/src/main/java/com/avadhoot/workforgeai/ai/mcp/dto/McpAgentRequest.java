package com.avadhoot.workforgeai.ai.mcp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record McpAgentRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message,

        @Min(1)
        @Max(10)
        Integer maxIterations,

        @Size(max = 64) String conversationId,

        @Size(max = 128) String sessionId
) {
    public McpAgentRequest(String message, Integer maxIterations) {
        this(message, maxIterations, null, null);
    }
}
