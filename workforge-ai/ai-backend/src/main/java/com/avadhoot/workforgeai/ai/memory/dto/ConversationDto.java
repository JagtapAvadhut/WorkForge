package com.avadhoot.workforgeai.ai.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
        UUID id,
        String sessionId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
