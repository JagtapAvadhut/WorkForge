package com.avadhoot.workforgeai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HistoryMessage(
        @NotBlank(message = "history.role is required")
        String role,

        @NotBlank(message = "history.content is required")
        @Size(max = 4000, message = "history.content must be at most 4000 characters")
        String content
) {
}
