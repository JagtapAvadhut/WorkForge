package com.avadhoot.workforgeai.ai.document;

import com.avadhoot.workforgeai.ai.document.controller.DocumentController;
import com.avadhoot.workforgeai.ai.document.dto.DocumentPageResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchHit;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchResponse;
import com.avadhoot.workforgeai.ai.document.service.DocumentIngestionService;
import com.avadhoot.workforgeai.ai.document.service.DocumentQueryService;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentControllerTest {

    private MockMvc mockMvc;
    private DocumentIngestionService ingestionService;
    private DocumentQueryService queryService;

    @BeforeEach
    void setUp() {
        ingestionService = mock(DocumentIngestionService.class);
        queryService = mock(DocumentQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new DocumentController(ingestionService, queryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void create_returnsCreatedDocument() throws Exception {
        UUID id = UUID.randomUUID();
        when(ingestionService.ingest(any())).thenReturn(new DocumentResponse(
                id, "A sprint is a fixed development period.", Map.of("topic", "sprint"),
                768, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(post("/api/v1/ai/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"A sprint is a fixed development period.","metadata":{"topic":"sprint"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.embeddingDimensions").value(768))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void search_returnsRankedHits() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.search(any())).thenReturn(new DocumentSearchResponse(
                "How does a sprint work?",
                5,
                List.of(new DocumentSearchHit(id, "A sprint is a fixed development period.", Map.of(), 0.88))));

        mockMvc.perform(post("/api/v1/ai/documents/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"How does a sprint work?","topK":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results[0].similarity").value(0.88));
    }

    @Test
    void list_supportsPaginationParams() throws Exception {
        when(queryService.list(isNull(), isNull())).thenReturn(new DocumentPageResponse(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/ai/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void delete_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/v1/ai/documents/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void search_rejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/v1/ai/documents/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"  ","topK":5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
