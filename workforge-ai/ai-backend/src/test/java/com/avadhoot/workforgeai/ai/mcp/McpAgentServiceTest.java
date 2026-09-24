package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentRequest;
import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentResponse;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpAgentServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private McpClientGateway gateway;
    private McpAgentService service;
    private ConversationMemoryFacade memoryFacade;
    private final AtomicInteger promptCount = new AtomicInteger();
    private final java.util.UUID conversationId = java.util.UUID.randomUUID();

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        gateway = mock(McpClientGateway.class);
        WorkforgeMcpAgentGraph graph = new WorkforgeMcpAgentGraph(chatClient, gateway, JsonMapper.builder().build());
        memoryFacade = mock(ConversationMemoryFacade.class);
        when(memoryFacade.prepare(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn(new ConversationMemoryFacade.PreparedContext(
                        conversationId, "default", java.util.List.of(), java.util.List.of(), ""));
        org.mockito.Mockito.doNothing().when(memoryFacade)
                .completeTurn(org.mockito.ArgumentMatchers.any(), anyString(), anyString());
        service = new McpAgentService(graph, gateway, memoryFacade, 5);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        promptCount.set(0);
    }

    @Test
    void noTool_completes() {
        when(gateway.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                true, true, "http://localhost:8091/mcp", "ok"));
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"answer\":\"A sprint is a fixed period.\"}");

        McpAgentResponse result = service.run(new McpAgentRequest("What is a sprint?", 3));

        assertThat(result.completed()).isTrue();
        assertThat(result.toolsUsed()).isEmpty();
        assertThat(result.steps()).extracting(s -> s.node())
                .contains(GraphNodeNames.ANALYZE_REQUEST, GraphNodeNames.DECIDE_ACTION, GraphNodeNames.FINALIZE);
        assertThat(result.conversationId()).isEqualTo(conversationId.toString());
    }

    @Test
    void conversationContinuation_persistsTurn() {
        when(gateway.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                true, true, "http://localhost:8091/mcp", "ok"));
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"answer\":\"Follow-up with prior context.\"}");

        McpAgentResponse result = service.run(new McpAgentRequest(
                "Continue about MWS-1", 3, conversationId.toString(), "session-1"));

        assertThat(result.conversationId()).isEqualTo(conversationId.toString());
        org.mockito.Mockito.verify(memoryFacade).prepare(
                conversationId.toString(), "session-1", "Continue about MWS-1");
        org.mockito.Mockito.verify(memoryFacade).completeTurn(
                conversationId, "Continue about MWS-1", result.response());
    }

    @Test
    void oneTool_viaMcp() {
        when(gateway.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                true, true, "http://localhost:8091/mcp", "ok"));
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
                default -> "{\"action\":\"final\",\"answer\":\"MWS-1 is a checkout bug.\"}";
            };
        });
        when(gateway.executeTool(eq("getIssue"), anyMap()))
                .thenReturn("{\"found\":true,\"issueKey\":\"MWS-1\",\"summary\":\"Fix checkout\"}");

        McpAgentResponse result = service.run(new McpAgentRequest("Show MWS-1", 4));

        assertThat(result.toolsUsed()).extracting(m -> m.get("tool")).contains("getIssue");
        assertThat(result.toolsUsed()).allSatisfy(m -> assertThat(m.get("via")).isEqualTo("mcp"));
        assertThat(result.steps()).extracting(s -> s.node())
                .contains(GraphNodeNames.EXECUTE_TOOL, GraphNodeNames.OBSERVE_RESULT);
    }

    @Test
    void unavailable_failsCleanly() {
        when(gateway.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                false, false, "http://localhost:8091/mcp", "down"));

        assertThatThrownBy(() -> service.run(new McpAgentRequest("Show MWS-1", 3)))
                .isInstanceOf(McpUnavailableException.class);
    }
}
