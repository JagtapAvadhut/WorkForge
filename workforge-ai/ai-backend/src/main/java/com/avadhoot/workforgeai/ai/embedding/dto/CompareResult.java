package com.avadhoot.workforgeai.ai.embedding.dto;

import java.util.List;

public record CompareResult(
        String query,
        List<RankedDocument> ranked
) {
}
