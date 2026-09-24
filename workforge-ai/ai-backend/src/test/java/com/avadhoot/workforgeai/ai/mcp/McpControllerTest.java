package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentStep;
import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentResponse;
import com.avadhoot.workforgeai.ai.mcp.dto.McpChatResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class McpControllerTest {

    private MockMvc mockMvc;
    private McpClientGateway gateway;
    private McpChatService chatService;
    private McpAgentService agentService;

    @BeforeEach
    void setUp() {
        gateway = mock(McpClientGateway.class);
        chatService = mock(McpChatService.class);
        agentService = mock(McpAgentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new McpController(gateway, chatService, agentService, new com.avadhoot.workforgeai.ai.pipeline.PassThroughAiRequestPipeline()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void tools_returnsDiscovered() throws Exception {
        when(gateway.discoverTools()).thenReturn(List.of(
                new McpClientGateway.McpToolDescriptor("getIssue", "Get issue", Map.of("type", "object"))));

        mockMvc.perform(get("/api/v1/ai/mcp/tools"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("getIssue"));
    }

    @Test
    void mcpChat_returnsResponse() throws Exception {
        when(chatService.chat(any())).thenReturn(new McpChatResponse("ok", List.of()));

        mockMvc.perform(post("/api/v1/ai/mcp-chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"What is a sprint?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.response").value("ok"));
    }

    @Test
    void mcpAgent_returnsResponse() throws Exception {
        when(agentService.run(any())).thenReturn(new McpAgentResponse(
                "MWS-1 bug", true, 2, "COMPLETED", GraphNodeNames.FINALIZE,
                List.of(new GraphAgentStep(GraphNodeNames.EXECUTE_TOOL, "getIssue")),
                List.of(Map.of("tool", "getIssue", "via", "mcp"))));

        mockMvc.perform(post("/api/v1/ai/mcp-agent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Show MWS-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolsUsed[0].tool").value("getIssue"));
    }

    @Test
    void unavailable_returns503() throws Exception {
        when(gateway.discoverTools()).thenThrow(new McpUnavailableException("MCP server unavailable"));

        mockMvc.perform(get("/api/v1/ai/mcp/tools"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false));
    }
}
