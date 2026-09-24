package com.avadhoot.workforgeai.ai.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpClientGatewayTest {

    private ToolCallbackProvider provider;
    private ObjectProvider<ToolCallbackProvider> objectProvider;
    private McpClientGateway gateway;

    @BeforeEach
    void setUp() {
        provider = mock(ToolCallbackProvider.class);
        objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getIfAvailable()).thenReturn(provider);
        gateway = new McpClientGateway(
                objectProvider,
                JsonMapper.builder().build(),
                "http://127.0.0.1:59999",
                "/mcp",
                "getIssue,searchIssues,getProject");
    }

    @Test
    void discoverTools_filtersAllowlist() {
        ToolCallback allowed = callback("getIssue", "Get issue", "{\"type\":\"object\"}");
        ToolCallback blocked = callback("deleteIssue", "Delete", "{\"type\":\"object\"}");
        when(provider.getToolCallbacks()).thenReturn(new ToolCallback[]{allowed, blocked});

        List<McpClientGateway.McpToolDescriptor> tools = gateway.discoverTools();

        assertThat(tools).extracting(McpClientGateway.McpToolDescriptor::name)
                .containsExactly("getIssue");
    }

    @Test
    void executeUnknownTool_fails() {
        when(provider.getToolCallbacks()).thenReturn(new ToolCallback[0]);

        assertThatThrownBy(() -> gateway.executeTool("getIssue", Map.of("issueKey", "MWS-1")))
                .isInstanceOf(McpToolException.class)
                .hasMessageContaining("Unknown MCP tool");
    }

    @Test
    void executeDisallowedTool_fails() {
        assertThatThrownBy(() -> gateway.executeTool("deleteIssue", Map.of()))
                .isInstanceOf(McpToolException.class)
                .hasMessageContaining("not allowlisted");
    }

    @Test
    void status_whenServerDown() {
        McpClientGateway.McpConnectionStatus status = gateway.status();
        assertThat(status.serverUp()).isFalse();
        assertThat(status.clientConnected()).isFalse();
    }

    private static ToolCallback callback(String name, String description, String schema) {
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(
                ToolDefinition.builder().name(name).description(description).inputSchema(schema).build());
        return callback;
    }
}
