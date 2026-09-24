package com.avadhoot.workforgeai.ai.embedding.service;

import com.avadhoot.workforgeai.ai.embedding.dto.CompareResult;
import com.avadhoot.workforgeai.ai.embedding.dto.RankedDocument;
import com.avadhoot.workforgeai.ai.embedding.dto.SimilarityResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Pure vector math + ranking over embeddings produced by {@link EmbeddingService}.
 */
@Service
public class EmbeddingSimilarityService {

    private final EmbeddingService embeddingService;

    public EmbeddingSimilarityService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public SimilarityResult similarity(String text1, String text2) {
        float[] a = embeddingService.embed(text1);
        float[] b = embeddingService.embed(text2);
        return new SimilarityResult(cosineSimilarity(a, b));
    }

    public CompareResult compare(String query, List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            throw new IllegalArgumentException("documents must not be empty");
        }

        float[] queryVector = embeddingService.embed(query);
        var batch = embeddingService.embedBatch(documents);
        List<RankedDocument> ranked = new ArrayList<>(batch.items().size());

        for (var item : batch.items()) {
            float[] docVector = toFloatArray(item.embedding());
            ranked.add(new RankedDocument(item.text(), cosineSimilarity(queryVector, docVector)));
        }

        ranked.sort(Comparator.comparingDouble(RankedDocument::similarity).reversed());
        return new CompareResult(query.trim(), List.copyOf(ranked));
    }

    private static float[] toFloatArray(List<Double> values) {
        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i).floatValue();
        }
        return vector;
    }

    /**
     * Cosine similarity in [-1, 1] for typical float embeddings (often near [0, 1] after normalization).
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("vectors must not be null");
        }
        if (a.length == 0 || b.length == 0) {
            throw new IllegalArgumentException("vectors must not be empty");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "vector dimension mismatch: " + a.length + " vs " + b.length);
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            double va = a[i];
            double vb = b[i];
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }

        if (normA == 0.0 || normB == 0.0) {
            throw new IllegalArgumentException("cannot compute cosine similarity for a zero vector");
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
