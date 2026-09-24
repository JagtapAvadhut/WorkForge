package com.avadhoot.workforgeai.ai.embedding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmbedTextRequest(
        @NotBlank(message = "text is required")
        @Size(max = 8000, message = "text must be at most 8000 characters")
        String text
) {
}
