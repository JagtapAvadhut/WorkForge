package com.avadhoot.workforgeai.ai.graph;

import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentRequest;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentResponse;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import com.avadhoot.workforgeai.ai.tools.WorkforgeTools;
import com.avadhoot.workforgeai.ai.tools.model.IssueRecord;
import com.avadhoot.workforgeai.ai.tools.model.ProjectRecord;
import com.avadhoot.workforgeai.ai.tools.service.IssueToolService;
import com.avadhoot.workforgeai.ai.tools.service.ProjectToolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentGraphServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private IssueToolService issueToolService;
    private ProjectToolService projectToolService;
    private AgentGraphService service;
    private ConversationMemoryFacade memoryFacade;
    private final AtomicInteger promptCount = new AtomicInteger();
    private final java.util.UUID conversationId = java.util.UUID.randomUUID();

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        issueToolService = mock(IssueToolService.class);
        projectToolService = mock(ProjectToolService.class);
        WorkforgeTools tools = new WorkforgeTools(issueToolService, projectToolService);
        WorkforgeAgentGraph graph = new WorkforgeAgentGraph(chatClient, tools, JsonMapper.builder().build());
        memoryFacade = mock(ConversationMemoryFacade.class);
        when(memoryFacade.prepare(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn(new ConversationMemoryFacade.PreparedContext(
                        conversationId, "default", List.of(), List.of(), ""));
        org.mockito.Mockito.doNothing().when(memoryFacade)
                .completeTurn(org.mockito.ArgumentMatchers.any(), anyString(), anyString());
        service = new AgentGraphService(graph, memoryFacade, 5);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        promptCount.set(0);
    }

    @Test
    void noToolRequest_goesAnalyzeDecideFinalize() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"answer\":\"A sprint is a fixed development period.\"}");

        GraphAgentResponse result = service.run(new GraphAgentRequest("What is a sprint?", 3));

        assertThat(result.completed()).isTrue();
        assertThat(result.status()).isEqualTo(WorkforgeAgentState.STATUS_COMPLETED);
        assertThat(result.iterations()).isEqualTo(1);
        assertThat(result.toolCalls()).isEmpty();
        assertThat(result.steps()).extracting(s -> s.node())
                .containsExactly(
                        GraphNodeNames.ANALYZE_REQUEST,
                        GraphNodeNames.DECIDE_ACTION,
                        GraphNodeNames.FINALIZE);
        assertThat(result.response()).contains("sprint");
        assertThat(result.conversationId()).isEqualTo(conversationId.toString());
    }

    @Test
    void conversationContinuation_returnsConversationIdAndPersistsTurn() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"answer\":\"Continuing from earlier context.\"}");

        GraphAgentResponse result = service.run(new GraphAgentRequest(
                "Continue", 3, conversationId.toString(), "session-1"));

        assertThat(result.conversationId()).isEqualTo(conversationId.toString());
        org.mockito.Mockito.verify(memoryFacade).prepare(
                conversationId.toString(), "session-1", "Continue");
        org.mockito.Mockito.verify(memoryFacade).completeTurn(
                conversationId, "Continue", result.response());
    }

    @Test
    void oneToolRequest_executesGetIssueThenFinalizes() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
                case 1 -> "{\"action\":\"final\",\"answer\":\"MWS-1 is a checkout bug.\"}";
                default -> "{\"action\":\"final\",\"answer\":\"done\"}";
            };
        });
        when(issueToolService.getIssue("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        GraphAgentResponse result = service.run(new GraphAgentRequest("Show me issue MWS-1", 4));

        assertThat(result.steps()).extracting(s -> s.node())
                .containsExactly(
                        GraphNodeNames.ANALYZE_REQUEST,
                        GraphNodeNames.DECIDE_ACTION,
                        GraphNodeNames.EXECUTE_TOOL,
                        GraphNodeNames.OBSERVE_RESULT,
                        GraphNodeNames.DECIDE_ACTION,
                        GraphNodeNames.FINALIZE);
        assertThat(result.toolCalls()).extracting(m -> m.get("tool")).containsExactly("getIssue");
        assertThat(result.toolResults()).isNotEmpty();
        assertThat(result.completed()).isTrue();
        assertThat(result.iterations()).isEqualTo(2);
    }

    @Test
    void multiStep_loopsDecideExecuteObserve() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
                case 1 -> "{\"action\":\"tool\",\"tool\":\"getProject\",\"arguments\":{\"projectKey\":\"MWS\"}}";
                case 2 -> "{\"action\":\"final\",\"answer\":\"MWS-1 belongs to Mobile Web Store.\"}";
                default -> "{\"action\":\"final\",\"answer\":\"done\"}";
            };
        });
        when(issueToolService.getIssue("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));
        when(projectToolService.getProject("MWS")).thenReturn(Optional.of(
                new ProjectRecord("MWS", "Mobile Web Store", "demo", "avadhoot", 5)));

        GraphAgentResponse result = service.run(new GraphAgentRequest("Tell me about MWS-1 and its project.", 5));

        assertThat(result.toolCalls()).extracting(m -> m.get("tool"))
                .containsExactly("getIssue", "getProject");
        long decideCount = result.steps().stream()
                .filter(s -> GraphNodeNames.DECIDE_ACTION.equals(s.node()))
                .count();
        assertThat(decideCount).isEqualTo(3);
        assertThat(result.steps()).extracting(s -> s.node())
                .contains(GraphNodeNames.OBSERVE_RESULT, GraphNodeNames.FINALIZE);
        assertThat(result.completed()).isTrue();
    }

    @Test
    void conditionalEdge_routesToolVsFinalize() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"answer\":\"Conceptual answer.\"}");

        GraphAgentResponse noTool = service.run(new GraphAgentRequest("What is backlog?", 2));
        assertThat(noTool.steps()).extracting(s -> s.node())
                .doesNotContain(GraphNodeNames.EXECUTE_TOOL);

        promptCount.set(0);
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"getProject\",\"arguments\":{\"projectKey\":\"MWS\"}}";
                default -> "{\"action\":\"final\",\"answer\":\"MWS project.\"}";
            };
        });
        when(projectToolService.getProject("MWS")).thenReturn(Optional.of(
                new ProjectRecord("MWS", "Mobile Web Store", "demo", "avadhoot", 5)));

        GraphAgentResponse withTool = service.run(new GraphAgentRequest("Tell me about MWS", 3));
        assertThat(withTool.steps()).extracting(s -> s.node())
                .contains(GraphNodeNames.EXECUTE_TOOL, GraphNodeNames.OBSERVE_RESULT);
    }

    @Test
    void maxIterations_stopsAndFinalizesSafely() {
        when(issueToolService.getIssue("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            if (n < 2) {
                return "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
            }
            return "Best available answer after max iterations.";
        });

        GraphAgentResponse result = service.run(new GraphAgentRequest("Keep fetching MWS-1", 2));

        assertThat(result.completed()).isTrue();
        assertThat(result.status()).isEqualTo(WorkforgeAgentState.STATUS_MAX_ITERATIONS);
        assertThat(result.iterations()).isGreaterThanOrEqualTo(2);
        assertThat(result.steps()).extracting(s -> s.node()).contains(GraphNodeNames.FINALIZE);
        assertThat(result.response()).contains("max iterations");
    }

    @Test
    void unknownTool_recordsFailureAndCanFinalize() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"deleteIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
                default -> "{\"action\":\"final\",\"answer\":\"Cannot delete; tools are read-only.\"}";
            };
        });

        GraphAgentResponse result = service.run(new GraphAgentRequest("Delete MWS-1", 3));

        assertThat(result.toolResults()).anySatisfy(r ->
                assertThat(r.get("ok")).isEqualTo(false));
        assertThat(result.status()).isIn(
                WorkforgeAgentState.STATUS_TOOL_FAILURE,
                WorkforgeAgentState.STATUS_COMPLETED);
        assertThat(result.completed()).isTrue();
        assertThat(result.response()).containsIgnoringCase("read-only");
    }

    @Test
    void statePropagatesUserMessageThroughNodes() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"answer\":\"ok\"}");

        GraphAgentResponse result = service.run(new GraphAgentRequest("propagate-me", 2));

        assertThat(result.steps().getFirst().node()).isEqualTo(GraphNodeNames.ANALYZE_REQUEST);
        assertThat(result.currentNode()).isEqualTo(GraphNodeNames.FINALIZE);
        assertThat(result.completed()).isTrue();
    }

    @Test
    void rejectsBlankMessage() {
        assertThatThrownBy(() -> service.run(new GraphAgentRequest("  ", 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }
}
