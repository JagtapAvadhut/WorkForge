package com.avadhoot.workforgeai.ai.rag.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RagQueryRequest(
        @NotBlank(message = "question is required")
        @Size(max = 4000, message = "question must be at most 4000 characters")
        String question,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 100, message = "topK must be at most 100")
        Integer topK
) {
}
