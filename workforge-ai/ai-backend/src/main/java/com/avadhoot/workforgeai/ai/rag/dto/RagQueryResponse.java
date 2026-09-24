package com.avadhoot.workforgeai.ai.rag.dto;

import java.util.List;

public record RagQueryResponse(
        String answer,
        List<RagSource> sources
) {
}
