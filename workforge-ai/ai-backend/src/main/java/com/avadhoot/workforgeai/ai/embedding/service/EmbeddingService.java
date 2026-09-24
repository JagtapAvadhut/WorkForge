package com.avadhoot.workforgeai.ai.embedding.service;

import com.avadhoot.workforgeai.ai.embedding.dto.BatchEmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.EmbeddingResult;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Local Ollama embedding wrapper. Chat model and embedding model are configured separately.
 */
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public float[] embed(String text) {
        String normalized = requireText(text, "text");
        float[] vector = embeddingModel.embed(normalized);
        requireVector(vector, "embedding");
        return vector;
    }

    public EmbeddingResult embedAsResult(String text) {
        String normalized = requireText(text, "text");
        float[] vector = embed(normalized);
        return toResult(normalized, vector);
    }

    public BatchEmbeddingResult embedBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be empty");
        }

        List<String> normalized = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i++) {
            normalized.add(requireText(texts.get(i), "texts[" + i + "]"));
        }

        // Single EmbeddingModel call for the batch — avoids re-initializing the client per item.
        List<float[]> vectors = embeddingModel.embed(normalized);
        if (vectors == null || vectors.size() != normalized.size()) {
            throw new IllegalStateException("Embedding model returned an unexpected batch size");
        }

        List<EmbeddingResult> items = new ArrayList<>(normalized.size());
        for (int i = 0; i < normalized.size(); i++) {
            float[] vector = vectors.get(i);
            requireVector(vector, "embeddings[" + i + "]");
            items.add(toResult(normalized.get(i), vector));
        }
        return new BatchEmbeddingResult(List.copyOf(items));
    }

    public static EmbeddingResult toResult(String text, float[] vector) {
        List<Double> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add((double) value);
        }
        return new EmbeddingResult(text, vector.length, List.copyOf(values));
    }

    private static String requireText(String text, String field) {
        if (text == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return text.trim();
    }

    private static void requireVector(float[] vector, String field) {
        if (vector == null || vector.length == 0) {
            throw new IllegalStateException(field + " is empty");
        }
    }
}
