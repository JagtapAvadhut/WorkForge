package com.avadhoot.workforgeai.ai.tools;

import com.avadhoot.workforgeai.ai.tools.controller.ToolChatController;
import com.avadhoot.workforgeai.ai.tools.dto.ToolCallInfo;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatResponse;
import com.avadhoot.workforgeai.ai.tools.dto.ToolPreviewResponse;
import com.avadhoot.workforgeai.ai.tools.service.ToolChatService;
import com.avadhoot.workforgeai.common.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ToolChatControllerTest {

    private MockMvc mockMvc;
    private ToolChatService toolChatService;

    @BeforeEach
    void setUp() {
        toolChatService = mock(ToolChatService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ToolChatController(toolChatService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void toolChat_returnsResponseAndToolCalls() throws Exception {
        when(toolChatService.chat(any())).thenReturn(new ToolChatResponse(
                "MWS has open issues assigned to Avadhoot.",
                List.of(new ToolCallInfo("searchIssues", Map.of("projectKey", "MWS", "status", "OPEN"), "{\"count\":2}"))));

        mockMvc.perform(post("/api/v1/ai/tool-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Show me open MWS issues assigned to Avadhoot"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.toolCalls[0].tool").value("searchIssues"))
                .andExpect(jsonPath("$.data.toolCalls[0].arguments.projectKey").value("MWS"));
    }

    @Test
    void toolPreview_returnsDefinitions() throws Exception {
        when(toolChatService.preview(any())).thenReturn(new ToolPreviewResponse(
                "Find open MWS issues",
                List.of(new ToolPreviewResponse.ToolDefinitionView(
                        "searchIssues",
                        "Search issues",
                        "{\"type\":\"object\"}"))));

        mockMvc.perform(post("/api/v1/ai/tool-preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Find open MWS issues"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tools[0].name").value("searchIssues"))
                .andExpect(jsonPath("$.data.tools[0].inputSchema").isNotEmpty());
    }

    @Test
    void toolChat_rejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/ai/tool-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
