package com.avadhoot.workforgeai.ai.document.dto;

import java.util.Map;
import java.util.UUID;

public record DocumentSearchHit(
        UUID id,
        String content,
        Map<String, Object> metadata,
        double similarity
) {
}
