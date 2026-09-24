package com.avadhoot.workforgeai.ai.embedding.dto;

import java.util.List;

public record BatchEmbeddingResult(List<EmbeddingResult> items) {
}
