package com.avadhoot.workforgeai.ai.embedding.dto;

import java.util.List;

public record EmbeddingResult(
        String text,
        int dimensions,
        List<Double> embedding
) {
}
