package com.avadhoot.workforgeai.ai.embedding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SimilarityRequest(
        @NotBlank(message = "text1 is required")
        @Size(max = 8000, message = "text1 must be at most 8000 characters")
        String text1,

        @NotBlank(message = "text2 is required")
        @Size(max = 8000, message = "text2 must be at most 8000 characters")
        String text2
) {
}
