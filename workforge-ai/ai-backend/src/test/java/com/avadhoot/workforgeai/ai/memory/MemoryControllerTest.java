package com.avadhoot.workforgeai.ai.memory;

import com.avadhoot.workforgeai.ai.memory.dto.ConversationDto;
import com.avadhoot.workforgeai.ai.memory.dto.MemoryDto;
import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemoryControllerTest {

    private MockMvc mockMvc;
    private ConversationService conversationService;
    private MemoryService memoryService;

    @BeforeEach
    void setUp() {
        conversationService = mock(ConversationService.class);
        memoryService = mock(MemoryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MemoryController(conversationService, memoryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createConversation() throws Exception {
        UUID id = UUID.randomUUID();
        when(conversationService.create(any(), any())).thenReturn(
                new ConversationRecord(id, "default", "New", Instant.now(), Instant.now()));

        mockMvc.perform(post("/api/v1/ai/memory/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    void rememberAndList() throws Exception {
        UUID id = UUID.randomUUID();
        MemoryRecord record = new MemoryRecord(
                id, "default", "preference", "User prefers simple explanations",
                "explicit", 5, true, Instant.now(), Instant.now());
        when(memoryService.remember(any(), anyString(), anyString(), any())).thenReturn(record);
        when(memoryService.list(any(), any(), any())).thenReturn(List.of(record));

        mockMvc.perform(post("/api/v1/ai/memory/remember")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"preference","content":"User prefers simple explanations","importance":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value("preference"));

        mockMvc.perform(get("/api/v1/ai/memory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id.toString()));
    }

    @Test
    void deleteMemory() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/ai/memory/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deleted").value(true));
    }
}
