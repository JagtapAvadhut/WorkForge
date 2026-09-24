package com.avadhoot.workforgeai.ai.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record MemoryDto(
        UUID id,
        String sessionId,
        String category,
        String content,
        String source,
        int importance,
        Instant createdAt,
        Instant updatedAt
) {
}
