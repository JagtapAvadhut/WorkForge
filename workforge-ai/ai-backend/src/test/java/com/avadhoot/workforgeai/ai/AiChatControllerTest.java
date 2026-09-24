package com.avadhoot.workforgeai.ai;

import com.avadhoot.workforgeai.ai.controller.AiChatController;
import com.avadhoot.workforgeai.ai.dto.ChatResponse;
import com.avadhoot.workforgeai.ai.dto.PromptPreviewResponse;
import com.avadhoot.workforgeai.ai.prompt.PromptStrategy;
import com.avadhoot.workforgeai.ai.prompt.PromptVersion;
import com.avadhoot.workforgeai.ai.service.AiChatService;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiChatControllerTest {

    private MockMvc mockMvc;
    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        aiChatService = mock(AiChatService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AiChatController(aiChatService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void chat_returnsStrategyInPayload() throws Exception {
        when(aiChatService.chat(any())).thenReturn(
                new ChatResponse("A sprint is a time box.", PromptStrategy.DOMAIN_EXPERT));

        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Explain sprint","strategy":"DOMAIN_EXPERT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.response").value("A sprint is a time box."))
                .andExpect(jsonPath("$.data.strategy").value("DOMAIN_EXPERT"));
    }

    @Test
    void promptPreview_endpointWorks() throws Exception {
        when(aiChatService.preview(any())).thenReturn(new PromptPreviewResponse(
                PromptStrategy.CONCISE,
                PromptVersion.CONCISE_V1,
                "system",
                List.of(new PromptPreviewResponse.PreviewMessage("user", "Explain sprint")),
                "Explain sprint"));

        mockMvc.perform(post("/api/v1/ai/prompt-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Explain sprint","strategy":"CONCISE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.strategy").value("CONCISE"))
                .andExpect(jsonPath("$.data.version").value("CONCISE_V1"));
    }

    @Test
    void chat_rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
