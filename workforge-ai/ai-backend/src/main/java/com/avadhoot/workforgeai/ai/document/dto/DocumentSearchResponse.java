package com.avadhoot.workforgeai.ai.document.dto;

import java.util.List;

public record DocumentSearchResponse(
        String query,
        int topK,
        List<DocumentSearchHit> results
) {
}
