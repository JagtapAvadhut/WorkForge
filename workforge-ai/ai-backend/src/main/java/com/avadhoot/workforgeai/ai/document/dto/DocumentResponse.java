package com.avadhoot.workforgeai.ai.document.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String content,
        Map<String, Object> metadata,
        int embeddingDimensions,
        Instant createdAt,
        Instant updatedAt
) {
}
