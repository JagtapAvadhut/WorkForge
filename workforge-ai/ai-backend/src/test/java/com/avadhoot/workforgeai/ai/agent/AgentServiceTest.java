package com.avadhoot.workforgeai.ai.agent;

import com.avadhoot.workforgeai.ai.agent.dto.AgentRequest;
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

class AgentServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private IssueToolService issueToolService;
    private ProjectToolService projectToolService;
    private AgentService agentService;
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
        memoryFacade = mock(ConversationMemoryFacade.class);
        when(memoryFacade.prepare(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn(new ConversationMemoryFacade.PreparedContext(
                        conversationId, "default", List.of(), List.of(), ""));
        org.mockito.Mockito.doNothing().when(memoryFacade)
                .completeTurn(org.mockito.ArgumentMatchers.any(), anyString(), anyString());
        agentService = new AgentService(chatClient, tools, JsonMapper.builder().build(), memoryFacade, 5);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        promptCount.set(0);
    }

    @Test
    void conceptualQuestion_finishesWithoutTools() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"thought\":\"conceptual\",\"answer\":\"A sprint is a fixed development period.\"}");

        AgentResult result = agentService.execute(new AgentRequest("What is a sprint?", 3));

        assertThat(result.answer()).contains("sprint");
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().getFirst().action()).isEqualTo("final");
        assertThat(result.steps().getFirst().tool()).isNull();
        assertThat(result.stopReason()).isEqualTo("completed");
    }

    @Test
    void multiStep_callsProjectThenIssuesDynamically() {
        when(callSpec.content()).thenAnswer(invocation -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"thought\":\"need project\",\"tool\":\"getProject\",\"arguments\":{\"projectKey\":\"MWS\"}}";
                case 1 -> "{\"action\":\"tool\",\"thought\":\"need open issues\",\"tool\":\"searchIssues\",\"arguments\":{\"projectKey\":\"MWS\",\"status\":\"OPEN\",\"limit\":5}}";
                case 2 -> "{\"action\":\"final\",\"thought\":\"enough data\",\"answer\":\"MWS is Mobile Web Store with open issues.\"}";
                default -> "{\"action\":\"final\",\"answer\":\"done\"}";
            };
        });
        when(projectToolService.getProject("MWS")).thenReturn(Optional.of(
                new ProjectRecord("MWS", "Mobile Web Store", "demo", "avadhoot", 5)));
        when(issueToolService.searchIssues("MWS", "OPEN", null, 5)).thenReturn(List.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        AgentResult result = agentService.execute(
                new AgentRequest("Tell me about project MWS and its open issues", 5));

        assertThat(result.steps()).extracting(AgentStep::action)
                .containsExactly("tool", "tool", "final");
        assertThat(result.steps()).extracting(AgentStep::tool)
                .containsExactly("getProject", "searchIssues", null);
        assertThat(result.answer()).contains("MWS");
        assertThat(result.stopReason()).isEqualTo("completed");
    }

    @Test
    void unknownTool_continuesAndCanFinish() {
        when(callSpec.content()).thenAnswer(invocation -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"deleteIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
                case 1 -> "{\"action\":\"final\",\"answer\":\"I cannot delete issues; tools are read-only.\"}";
                default -> "{\"action\":\"final\",\"answer\":\"done\"}";
            };
        });

        AgentResult result = agentService.execute(new AgentRequest("Delete MWS-1", 3));

        assertThat(result.steps().getFirst().status()).isEqualTo("unknown_tool");
        assertThat(result.answer()).contains("read-only");
    }

    @Test
    void maxSteps_forcesSynthesis() {
        when(callSpec.content()).thenAnswer(invocation -> {
            int n = promptCount.getAndIncrement();
            // Always request another tool until max steps, then synthesizeFinal uses next content.
            if (n < 2) {
                return "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
            }
            return "Synthesized after max steps using observations.";
        });
        when(issueToolService.getIssue("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        AgentResult result = agentService.execute(new AgentRequest("Show MWS-1 repeatedly", 2));

        assertThat(result.stopReason()).isEqualTo("max_steps");
        assertThat(result.iterations()).isEqualTo(2);
        assertThat(result.answer()).contains("Synthesized");
    }

    @Test
    void rejectsBlankMessage() {
        assertThatThrownBy(() -> agentService.execute(new AgentRequest("  ", 3)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
    }

    @Test
    void conversationContinuation_loadsHistoryAndPersistsTurnOnly() {
        when(memoryFacade.prepare(org.mockito.ArgumentMatchers.eq(conversationId.toString()), anyString(), anyString()))
                .thenReturn(new ConversationMemoryFacade.PreparedContext(
                        conversationId,
                        "session-1",
                        List.of(new com.avadhoot.workforgeai.ai.dto.HistoryMessage("user", "What is RAG?"),
                                new com.avadhoot.workforgeai.ai.dto.HistoryMessage("assistant", "RAG retrieves docs.")),
                        List.of(),
                        ""));
        when(callSpec.content()).thenReturn(
                "{\"action\":\"final\",\"thought\":\"follow-up\",\"answer\":\"For example, search docs then answer.\"}");

        var response = agentService.run(new AgentRequest(
                "Explain it using a simple example.", 3, conversationId.toString(), "session-1"));

        assertThat(response.conversationId()).isEqualTo(conversationId.toString());
        assertThat(response.answer()).contains("example");
        org.mockito.Mockito.verify(memoryFacade).prepare(
                conversationId.toString(), "session-1", "Explain it using a simple example.");
        org.mockito.Mockito.verify(memoryFacade).completeTurn(
                conversationId, "Explain it using a simple example.", response.answer());
        // Steps include thought but completeTurn only gets final answer — no CoT persistence API
        assertThat(response.steps().getFirst().thought()).isEqualTo("follow-up");
    }

    @Test
    void decisionParser_readsToolAndFinal() {
        JsonMapper mapper = JsonMapper.builder().build();
        var tool = AgentDecisionParser.parse(
                "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}",
                mapper);
        assertThat(tool).isPresent();
        assertThat(tool.get().isTool()).isTrue();
        assertThat(tool.get().tool()).isEqualTo("getIssue");

        var fin = AgentDecisionParser.parse(
                "{\"action\":\"final\",\"answer\":\"done\"}",
                mapper);
        assertThat(fin).isPresent();
        assertThat(fin.get().isFinal()).isTrue();
    }

    @Test
    void invalidJson_reasksThenExecutesTool() {
        when(callSpec.content()).thenAnswer(invocation -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"tool\",\"tool\":\"getIssue\""; // broken JSON
                case 1 -> "{\"action\":\"tool\",\"tool\":\"getIssue\",\"arguments\":{\"issueKey\":\"MWS-1\"}}";
                case 2 -> "{\"action\":\"final\",\"answer\":\"MWS-1 is a checkout bug.\"}";
                default -> "{\"action\":\"final\",\"answer\":\"done\"}";
            };
        });
        when(issueToolService.getIssue("MWS-1")).thenReturn(Optional.of(
                new IssueRecord("MWS-1", "Fix checkout", "OPEN", "HIGH", "avadhoot", "MWS", "Bug", "Sprint 12")));

        AgentResult result = agentService.execute(new AgentRequest("Show me issue MWS-1", 4));

        assertThat(result.steps()).extracting(AgentStep::tool)
                .contains("getIssue");
        assertThat(result.answer()).contains("MWS-1");
        assertThat(result.stopReason()).isEqualTo("completed");
    }
}
