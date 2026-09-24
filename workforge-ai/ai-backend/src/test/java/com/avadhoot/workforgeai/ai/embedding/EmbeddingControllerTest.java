package com.avadhoot.workforgeai.ai.embedding;

import com.avadhoot.workforgeai.ai.embedding.controller.EmbeddingController;
import com.avadhoot.workforgeai.ai.embedding.dto.BatchEmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.CompareResult;
import com.avadhoot.workforgeai.ai.embedding.dto.EmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.RankedDocument;
import com.avadhoot.workforgeai.ai.embedding.dto.SimilarityResult;
import com.avadhoot.workforgeai.ai.embedding.service.EmbeddingService;
import com.avadhoot.workforgeai.ai.embedding.service.EmbeddingSimilarityService;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmbeddingControllerTest {

    private MockMvc mockMvc;
    private EmbeddingService embeddingService;
    private EmbeddingSimilarityService similarityService;

    @BeforeEach
    void setUp() {
        embeddingService = mock(EmbeddingService.class);
        similarityService = mock(EmbeddingSimilarityService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new EmbeddingController(embeddingService, similarityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void embed_endpointReturnsVector() throws Exception {
        when(embeddingService.embedAsResult("What is a sprint?"))
                .thenReturn(new EmbeddingResult("What is a sprint?", 3, List.of(0.1, 0.2, 0.3)));

        mockMvc.perform(post("/api/v1/ai/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"What is a sprint?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dimensions").value(3))
                .andExpect(jsonPath("$.data.embedding[0]").value(0.1));
    }

    @Test
    void similarity_endpointWorks() throws Exception {
        when(similarityService.similarity(anyString(), anyString()))
                .thenReturn(new SimilarityResult(0.91));

        mockMvc.perform(post("/api/v1/ai/embeddings/similarity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text1":"How do I create a sprint?","text2":"How can I start a new sprint?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.similarity").value(0.91));
    }

    @Test
    void batch_endpointWorks() throws Exception {
        when(embeddingService.embedBatch(anyList())).thenReturn(new BatchEmbeddingResult(List.of(
                new EmbeddingResult("a", 2, List.of(1.0, 0.0))
        )));

        mockMvc.perform(post("/api/v1/ai/embeddings/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"texts":["a"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].dimensions").value(2));
    }

    @Test
    void compare_endpointWorks() throws Exception {
        when(similarityService.compare(anyString(), anyList())).thenReturn(new CompareResult(
                "How do I create a sprint?",
                List.of(new RankedDocument("A sprint is a fixed development period.", 0.88))
        ));

        mockMvc.perform(post("/api/v1/ai/embeddings/compare")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"How do I create a sprint?",
                                  "documents":["A sprint is a fixed development period."]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ranked[0].similarity").value(0.88));
    }

    @Test
    void embed_rejectsBlankText() throws Exception {
        mockMvc.perform(post("/api/v1/ai/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
