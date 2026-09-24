package com.avadhoot.workforgeai.ai.memory.model;

import java.time.Instant;
import java.util.UUID;

public record MessageRecord(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        Instant createdAt
) {
}
