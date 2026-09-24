package com.avadhoot.workforgeai.ai.mcp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record McpChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message
) {
}
