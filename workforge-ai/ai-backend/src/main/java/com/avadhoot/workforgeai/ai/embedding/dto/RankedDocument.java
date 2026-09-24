package com.avadhoot.workforgeai.ai.embedding.dto;

public record RankedDocument(
        String text,
        double similarity
) {
}
