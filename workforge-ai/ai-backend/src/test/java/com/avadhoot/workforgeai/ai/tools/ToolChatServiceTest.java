package com.avadhoot.workforgeai.ai.tools;

import com.avadhoot.workforgeai.ai.tools.dto.ToolChatRequest;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatResponse;
import com.avadhoot.workforgeai.ai.tools.dto.ToolPreviewResponse;
import com.avadhoot.workforgeai.ai.tools.service.IssueToolService;
import com.avadhoot.workforgeai.ai.tools.service.ProjectToolService;
import com.avadhoot.workforgeai.ai.tools.service.ToolChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ToolChatServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private ToolChatService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);

        IssueToolService issueService = mock(IssueToolService.class);
        ProjectToolService projectService = mock(ProjectToolService.class);
        WorkforgeTools tools = new WorkforgeTools(issueService, projectService);
        service = new ToolChatService(chatClient, tools, JsonMapper.builder().build());

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.toolCallbacks(anyList())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void chat_returnsResponseAndEmptyToolCalls_whenModelDoesNotInvokeTools() {
        when(callSpec.content()).thenReturn("A sprint is a fixed development period.");

        ToolChatResponse response = service.chat(new ToolChatRequest("What is a sprint?"));

        assertThat(response.response()).contains("sprint");
        assertThat(response.toolCalls()).isEmpty();
        verify(requestSpec).toolCallbacks(anyList());
    }

    @Test
    void chat_rejectsBlankMessage() {
        assertThatThrownBy(() -> service.chat(new ToolChatRequest("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }

    @Test
    void preview_returnsToolDefinitionsWithoutExecution() {
        ToolPreviewResponse preview = service.preview(new ToolChatRequest("Find open MWS issues"));

        assertThat(preview.message()).isEqualTo("Find open MWS issues");
        assertThat(preview.tools()).extracting(ToolPreviewResponse.ToolDefinitionView::name)
                .contains("getIssue", "searchIssues", "getProject");
        assertThat(preview.tools()).allSatisfy(tool -> {
            assertThat(tool.description()).isNotBlank();
            assertThat(tool.inputSchema()).isNotBlank();
        });
    }

    @Test
    void textParser_extractsToolCallJson() {
        var parsed = ToolCallTextParser.parse(
                "{\"name\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}",
                JsonMapper.builder().build());
        assertThat(parsed).isPresent();
        assertThat(parsed.get().name()).isEqualTo("getIssue");
        assertThat(parsed.get().argumentsJson()).contains("MWS-1");
    }

    @Test
    void recordingCallback_capturesArgumentsAndSummary() {
        List<RecordingToolCallback.RecordedToolCall> sink = RecordingToolCallback.newSink();
        org.springframework.ai.tool.ToolCallback delegate = mock(org.springframework.ai.tool.ToolCallback.class);
        when(delegate.getToolDefinition()).thenReturn(
                org.springframework.ai.tool.definition.ToolDefinition.builder()
                        .name("searchIssues")
                        .description("search")
                        .inputSchema("{\"type\":\"object\"}")
                        .build());
        when(delegate.call(anyString())).thenReturn("{\"count\":1}");

        org.springframework.ai.tool.ToolCallback wrapped =
                new RecordingToolCallback(delegate, sink, JsonMapper.builder().build());
        wrapped.call("{\"projectKey\":\"MWS\",\"status\":\"OPEN\"}");

        assertThat(sink).hasSize(1);
        assertThat(sink.getFirst().tool()).isEqualTo("searchIssues");
        assertThat(sink.getFirst().arguments()).containsEntry("projectKey", "MWS");
        assertThat(sink.getFirst().resultSummary()).contains("count");
    }
}
