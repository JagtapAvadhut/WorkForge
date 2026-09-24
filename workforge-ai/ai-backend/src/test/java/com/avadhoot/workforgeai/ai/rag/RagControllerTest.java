package com.avadhoot.workforgeai.ai.rag;

import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagControllerTest {

    private MockMvc mockMvc;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        ragService = mock(RagService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RagController(ragService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void query_returnsAnswerAndSources() throws Exception {
        UUID id = UUID.randomUUID();
        when(ragService.query(any())).thenReturn(new RagQueryResponse(
                "A sprint is a fixed development period.",
                List.of(new RagSource(id, "A sprint is a fixed development period.", Map.of("topic", "sprint"), 0.81))));

        mockMvc.perform(post("/api/v1/ai/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"What is a sprint?","topK":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.answer").value("A sprint is a fixed development period."))
                .andExpect(jsonPath("$.data.sources[0].similarity").value(0.81))
                .andExpect(jsonPath("$.data.sources[0].id").value(id.toString()));
    }

    @Test
    void query_rejectsBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/v1/ai/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"  ","topK":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
