package com.avadhoot.workforgeai.ai.embedding;

import com.avadhoot.workforgeai.ai.embedding.dto.BatchEmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.CompareResult;
import com.avadhoot.workforgeai.ai.embedding.dto.EmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.RankedDocument;
import com.avadhoot.workforgeai.ai.embedding.service.EmbeddingService;
import com.avadhoot.workforgeai.ai.embedding.service.EmbeddingSimilarityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmbeddingServiceTest {

    private EmbeddingModel embeddingModel;
    private EmbeddingService embeddingService;
    private EmbeddingSimilarityService similarityService;

    @BeforeEach
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingService = new EmbeddingService(embeddingModel);
        similarityService = new EmbeddingSimilarityService(embeddingService);
    }

    @Test
    void embed_returnsDimensionsFromVector() {
        when(embeddingModel.embed("What is a sprint?")).thenReturn(new float[]{0.1f, 0.2f, 0.3f});

        EmbeddingResult result = embeddingService.embedAsResult("What is a sprint?");

        assertThat(result.text()).isEqualTo("What is a sprint?");
        assertThat(result.dimensions()).isEqualTo(3);
        assertThat(result.embedding()).hasSize(3);
        assertThat(result.embedding().get(0)).isCloseTo(0.1, within(1e-6));
        assertThat(result.embedding().get(1)).isCloseTo(0.2, within(1e-6));
        assertThat(result.embedding().get(2)).isCloseTo(0.3, within(1e-6));
    }

    @Test
    void embed_rejectsBlankAndNull() {
        assertThatThrownBy(() -> embeddingService.embed("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        assertThatThrownBy(() -> embeddingService.embed(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void cosine_identicalVectors_nearOne() {
        float[] v = {1f, 2f, 3f};
        assertThat(EmbeddingSimilarityService.cosineSimilarity(v, v))
                .isCloseTo(1.0, within(1e-9));
    }

    @Test
    void cosine_orthogonalVectors_nearZero() {
        float[] a = {1f, 0f};
        float[] b = {0f, 1f};
        assertThat(EmbeddingSimilarityService.cosineSimilarity(a, b))
                .isCloseTo(0.0, within(1e-9));
    }

    @Test
    void cosine_rejectsDimensionMismatch() {
        assertThatThrownBy(() -> EmbeddingSimilarityService.cosineSimilarity(
                new float[]{1f, 2f}, new float[]{1f}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dimension mismatch");
    }

    @Test
    void cosine_rejectsZeroVector() {
        assertThatThrownBy(() -> EmbeddingSimilarityService.cosineSimilarity(
                new float[]{0f, 0f}, new float[]{1f, 2f}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zero vector");
    }

    @Test
    void cosine_rejectsNullOrEmpty() {
        assertThatThrownBy(() -> EmbeddingSimilarityService.cosineSimilarity(null, new float[]{1f}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmbeddingSimilarityService.cosineSimilarity(new float[]{}, new float[]{1f}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void batch_usesSingleModelCall() {
        when(embeddingModel.embed(anyList())).thenReturn(List.of(
                new float[]{1f, 0f},
                new float[]{0f, 1f},
                new float[]{1f, 1f}
        ));

        BatchEmbeddingResult result = embeddingService.embedBatch(List.of(
                "What is a sprint?",
                "What is a backlog?",
                "How do I create a bug?"
        ));

        assertThat(result.items()).hasSize(3);
        assertThat(result.items().get(0).dimensions()).isEqualTo(2);
        verify(embeddingModel, times(1)).embed(anyList());
    }

    @Test
    void compare_ranksBySimilarityDescending() {
        when(embeddingModel.embed("How do I create a sprint?")).thenReturn(new float[]{1f, 0f, 0f});
        when(embeddingModel.embed(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<String> texts = invocation.getArgument(0);
            return texts.stream().map(text -> switch (text) {
                case "A sprint is a fixed development period." -> new float[]{0.9f, 0.1f, 0f};
                case "A backlog contains planned work." -> new float[]{0.1f, 0.9f, 0f};
                case "A bug describes incorrect system behavior." -> new float[]{0f, 0.1f, 0.9f};
                default -> new float[]{0f, 0f, 1f};
            }).toList();
        });

        CompareResult result = similarityService.compare(
                "How do I create a sprint?",
                List.of(
                        "A backlog contains planned work.",
                        "A sprint is a fixed development period.",
                        "A bug describes incorrect system behavior."
                ));

        assertThat(result.ranked()).extracting(RankedDocument::text)
                .containsExactly(
                        "A sprint is a fixed development period.",
                        "A backlog contains planned work.",
                        "A bug describes incorrect system behavior."
                );
        assertThat(result.ranked().get(0).similarity())
                .isGreaterThan(result.ranked().get(1).similarity());
    }

    @Test
    void similarity_usesEmbeddedTexts() {
        when(embeddingModel.embed("alpha")).thenReturn(new float[]{1f, 0f});
        when(embeddingModel.embed("beta")).thenReturn(new float[]{1f, 0f});

        assertThat(similarityService.similarity("alpha", "beta").similarity())
                .isCloseTo(1.0, within(1e-9));
    }
}
