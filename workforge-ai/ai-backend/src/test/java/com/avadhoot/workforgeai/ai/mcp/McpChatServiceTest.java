package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.mcp.dto.McpChatRequest;
import com.avadhoot.workforgeai.ai.mcp.dto.McpChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpChatServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private McpClientGateway gateway;
    private McpChatService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        gateway = mock(McpClientGateway.class);
        service = new McpChatService(chatClient, gateway, JsonMapper.builder().build());

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(org.mockito.ArgumentMatchers.<List<ToolCallback>>any())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void conceptual_noTools() {
        when(gateway.allowedToolCallbacks()).thenReturn(List.of());
        when(gateway.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                true, true, "http://localhost:8091/mcp", "ok"));
        when(callSpec.content()).thenReturn("A sprint is a fixed development period.");

        McpChatResponse response = service.chat(new McpChatRequest("What is a sprint?"));

        assertThat(response.response()).contains("sprint");
        assertThat(response.toolsUsed()).isEmpty();
    }

    @Test
    void unavailableServer_failsCleanly() {
        when(gateway.allowedToolCallbacks()).thenReturn(List.of());
        when(gateway.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                false, false, "http://localhost:8091/mcp", "down"));

        assertThatThrownBy(() -> service.chat(new McpChatRequest("Show MWS-1")))
                .isInstanceOf(McpUnavailableException.class);
    }
}
