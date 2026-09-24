package com.avadhoot.workforgeai.ai.rag.dto;

import java.util.Map;
import java.util.UUID;

public record RagSource(
        UUID id,
        String content,
        Map<String, Object> metadata,
        double similarity
) {
}
