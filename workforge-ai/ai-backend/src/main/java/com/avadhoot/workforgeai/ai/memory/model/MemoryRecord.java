package com.avadhoot.workforgeai.ai.memory.model;

import java.time.Instant;
import java.util.UUID;

public record MemoryRecord(
        UUID id,
        String sessionId,
        String category,
        String content,
        String source,
        int importance,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
