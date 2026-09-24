package com.avadhoot.workforgeai.ai.embedding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompareRequest(
        @NotBlank(message = "query is required")
        @Size(max = 8000, message = "query must be at most 8000 characters")
        String query,

        @NotEmpty(message = "documents must not be empty")
        @Size(max = 32, message = "documents must contain at most 32 items")
        List<@NotBlank(message = "document text must not be blank")
                @Size(max = 8000, message = "each document must be at most 8000 characters") String> documents
) {
}
