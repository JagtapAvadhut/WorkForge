package com.avadhoot.workforgeai.ai.memory.dto;

import java.time.Instant;
import java.util.UUID;

public record MessageDto(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        Instant createdAt
) {
}
