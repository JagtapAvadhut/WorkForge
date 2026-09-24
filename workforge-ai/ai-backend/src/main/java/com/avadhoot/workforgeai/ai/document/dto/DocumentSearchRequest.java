package com.avadhoot.workforgeai.ai.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentSearchRequest(
        @NotBlank(message = "query is required")
        @Size(max = 4000, message = "query must be at most 4000 characters")
        String query,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 100, message = "topK must be at most 100")
        Integer topK
) {
}
