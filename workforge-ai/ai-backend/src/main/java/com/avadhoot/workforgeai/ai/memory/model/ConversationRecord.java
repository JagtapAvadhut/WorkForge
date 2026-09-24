package com.avadhoot.workforgeai.ai.memory.model;

import java.time.Instant;
import java.util.UUID;

public record ConversationRecord(
        UUID id,
        String sessionId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
