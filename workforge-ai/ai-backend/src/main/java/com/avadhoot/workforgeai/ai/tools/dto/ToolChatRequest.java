package com.avadhoot.workforgeai.ai.tools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ToolChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message
) {
}
