package com.avadhoot.workforgeai.ai.embedding.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchEmbedRequest(
        @NotEmpty(message = "texts must not be empty")
        @Size(max = 32, message = "texts must contain at most 32 items")
        List<@Size(max = 8000, message = "each text must be at most 8000 characters") String> texts
) {
}
