package com.avadhoot.workforgeai.ai.memory.dto;

import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @Size(max = 128) String sessionId,
        @Size(max = 300) String title
) {
}
