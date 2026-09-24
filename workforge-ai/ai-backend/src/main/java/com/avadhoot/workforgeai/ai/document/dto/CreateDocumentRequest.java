package com.avadhoot.workforgeai.ai.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CreateDocumentRequest(
        @NotBlank(message = "content is required")
        @Size(max = 20_000, message = "content must be at most 20000 characters")
        String content,

        Map<String, Object> metadata
) {
}
