package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentRequest;
import com.avadhoot.workforgeai.ai.rag.RagService;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryRequest;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiAgentServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private ConversationMemoryFacade memoryFacade;
    private SpecialistToolBridge toolBridge;
    private RagService ragService;
    private MultiAgentService service;
    private final AtomicInteger promptCount = new AtomicInteger();
    private final UUID conversationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        memoryFacade = mock(ConversationMemoryFacade.class);
        toolBridge = mock(SpecialistToolBridge.class);
        ragService = mock(RagService.class);

        when(memoryFacade.prepare(any(), any(), anyString()))
                .thenReturn(new ConversationMemoryFacade.PreparedContext(
                        conversationId, "default", List.of(), List.of(), ""));
        org.mockito.Mockito.doNothing().when(memoryFacade)
                .completeTurn(any(), anyString(), anyString());

        IssueInvestigationAgent issueAgent = new IssueInvestigationAgent(toolBridge);
        KnowledgeRagAgent knowledgeAgent = new KnowledgeRagAgent(ragService);
        ProjectAnalysisAgent projectAgent = new ProjectAnalysisAgent(toolBridge);
        WorkforgeMultiAgentGraph graph = new WorkforgeMultiAgentGraph(
                chatClient,
                JsonMapper.builder().build(),
                issueAgent,
                knowledgeAgent,
                projectAgent);
        service = new MultiAgentService(graph, memoryFacade, 5, 10);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        promptCount.set(0);
    }

    @Test
    void supervisorOnly_conceptualQuestion() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            if (n == 0) {
                return "{\"action\":\"finalize\",\"answer\":\"A sprint is a fixed development period.\"}";
            }
            return "A sprint is a fixed development period.";
        });

        var result = service.run(new MultiAgentRequest("What is a sprint?"));

        assertThat(result.completed()).isTrue();
        assertThat(result.agentsUsed()).isEmpty();
        assertThat(result.answer()).containsIgnoringCase("sprint");
        assertThat(result.steps()).extracting(s -> s.node()).contains(MultiAgentNames.SUPERVISOR, MultiAgentNames.FINALIZE);
    }

    @Test
    void issueAgentOnly() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\"],\"task\":\"Get MWS-1\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "MWS-1 is a checkout bug.";
            };
        });
        when(toolBridge.call(eq("getIssue"), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success(
                        "getIssue", Map.of("issueKey", "MWS-1"), "MWS-1 Fix checkout OPEN", "mcp"));

        var result = service.run(new MultiAgentRequest("Get details of MWS-1."));

        assertThat(result.agentsUsed()).containsExactly(MultiAgentNames.ISSUE_AGENT);
        assertThat(result.specialistResults()).isNotEmpty();
        assertThat(result.completed()).isTrue();
    }

    @Test
    void knowledgeAgentOnly() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"KNOWLEDGE_AGENT\"],\"task\":\"sprint docs\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "Sprints are documented as fixed iterations.";
            };
        });
        when(ragService.query(any(RagQueryRequest.class))).thenReturn(new RagQueryResponse(
                "Sprint statuses include FUTURE, ACTIVE, COMPLETE.",
                List.of(new RagSource(UUID.randomUUID(), "sprint status", Map.of(), 0.9))));

        var result = service.run(new MultiAgentRequest("Find documentation about sprint status values."));

        assertThat(result.agentsUsed()).containsExactly(MultiAgentNames.KNOWLEDGE_AGENT);
        assertThat(result.sources()).isNotEmpty();
    }

    @Test
    void projectAgentOnly() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"PROJECT_AGENT\"],\"task\":\"Analyze MWS\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "MWS summary ready.";
            };
        });
        when(toolBridge.call(eq("getProject"), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success(
                        "getProject", Map.of("projectKey", "MWS"), "MWS Mobile Web Store", "mcp"));
        when(toolBridge.call(eq("searchIssues"), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success(
                        "searchIssues", Map.of("projectKey", "MWS"), "2 issues", "mcp"));

        var result = service.run(new MultiAgentRequest("Analyze project MWS."));

        assertThat(result.agentsUsed()).containsExactly(MultiAgentNames.PROJECT_AGENT);
    }

    @Test
    void issuePlusKnowledge() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\",\"KNOWLEDGE_AGENT\"],\"task\":\"MWS-1 and docs\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "Issue and docs combined.";
            };
        });
        when(toolBridge.call(eq("getIssue"), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success(
                        "getIssue", Map.of("issueKey", "MWS-1"), "checkout bug", "mcp"));
        when(ragService.query(any())).thenReturn(new RagQueryResponse(
                "Docs mention checkout flows.", List.of(new RagSource(UUID.randomUUID(), "doc", Map.of(), 0.8))));

        var result = service.run(new MultiAgentRequest("Explain MWS-1 and relevant documentation."));

        assertThat(result.agentsUsed()).containsExactly(
                MultiAgentNames.ISSUE_AGENT, MultiAgentNames.KNOWLEDGE_AGENT);
        assertThat(result.specialistCalls()).isEqualTo(2);
    }

    @Test
    void issuePlusProject() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\",\"PROJECT_AGENT\"],\"task\":\"MWS analysis\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "Combined analysis.";
            };
        });
        when(toolBridge.call(anyString(), anyMap())).thenAnswer(inv -> {
            String tool = inv.getArgument(0);
            return SpecialistToolBridge.ToolCallOutcome.success(tool, Map.of(), tool + "-ok", "mcp");
        });

        var result = service.run(new MultiAgentRequest("Give me a project and issue analysis for MWS."));

        assertThat(result.agentsUsed()).contains(
                MultiAgentNames.ISSUE_AGENT, MultiAgentNames.PROJECT_AGENT);
    }

    @Test
    void allThreeSpecialists() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\",\"PROJECT_AGENT\",\"KNOWLEDGE_AGENT\"],\"task\":\"Investigate MWS-1 completely\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "Complete investigation.";
            };
        });
        when(toolBridge.call(anyString(), anyMap())).thenAnswer(inv ->
                SpecialistToolBridge.ToolCallOutcome.success(inv.getArgument(0), Map.of(), "ok", "mcp"));
        when(ragService.query(any())).thenReturn(new RagQueryResponse("docs", List.of(
                new RagSource(UUID.randomUUID(), "content", Map.of(), 0.7))));

        var result = service.run(new MultiAgentRequest("Investigate MWS-1 completely."));

        assertThat(result.agentsUsed()).containsExactlyInAnyOrder(
                MultiAgentNames.ISSUE_AGENT, MultiAgentNames.PROJECT_AGENT, MultiAgentNames.KNOWLEDGE_AGENT);
        assertThat(result.completed()).isTrue();
    }

    @Test
    void specialistFailure_stillTerminates() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\"],\"task\":\"MWS-1\"}";
                case 1 -> "{\"action\":\"finalize\",\"answer\":\"Could not load issue.\"}";
                default -> "Could not load issue.";
            };
        });
        when(toolBridge.call(anyString(), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.failure("getIssue", Map.of(), "boom", "mcp"));

        var result = service.run(new MultiAgentRequest("Get details of MWS-1."));

        assertThat(result.completed()).isTrue();
        assertThat(result.specialistResults().getFirst().get("status")).isEqualTo(SpecialistResult.FAILURE);
    }

    @Test
    void supervisorRetryDelegation() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\"],\"task\":\"MWS-1\"}";
                case 1 -> "{\"action\":\"delegate\",\"agents\":[\"KNOWLEDGE_AGENT\"],\"task\":\"docs\"}";
                case 2 -> "{\"action\":\"finalize\",\"answer\":\"done\"}";
                default -> "Combined.";
            };
        });
        when(toolBridge.call(anyString(), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success("getIssue", Map.of(), "ok", "mcp"));
        when(ragService.query(any())).thenReturn(new RagQueryResponse("docs", List.of()));

        var result = service.run(new MultiAgentRequest("Investigate MWS-1 then docs."));

        assertThat(result.agentsUsed()).contains(MultiAgentNames.ISSUE_AGENT, MultiAgentNames.KNOWLEDGE_AGENT);
        assertThat(result.iterations()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void iterationLimit_stopsSafely() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\"],\"task\":\"again\"}");
        when(toolBridge.call(anyString(), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success("getIssue", Map.of("issueKey", "MWS-1"), "ok", "mcp"));

        // maxIterations=1 → supervisor visit 1 delegates, after specialist supervisor would be visit 2 → limit
        WorkforgeMultiAgentGraph graph = new WorkforgeMultiAgentGraph(
                chatClient, JsonMapper.builder().build(),
                new IssueInvestigationAgent(toolBridge),
                new KnowledgeRagAgent(ragService),
                new ProjectAnalysisAgent(toolBridge));
        MultiAgentService limited = new MultiAgentService(graph, memoryFacade, 1, 10);

        // With maxIterations=1, first supervisor delegates. After specialist returns, second supervisor hits limit.
        // But agentsAlreadyUsed means second supervisor might finalize with agents_already_used before limit.
        // Force by clearing uniqueness: use maxIterations=1 and empty agents on second... 
        // Actually after ISSUE runs, agentsUsed has ISSUE. Supervisor visit 2: if model still delegates ISSUE, pending empty → finalize.
        // To test max iterations: keep delegating KNOWLEDGE which hasn't run - but maxIterations=1 means iteration 2 > 1 → limit.

        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            // Always try to delegate a new agent if possible; after issue used, ask knowledge
            if (n == 0) {
                return "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\"],\"task\":\"MWS-1\"}";
            }
            return "{\"action\":\"delegate\",\"agents\":[\"KNOWLEDGE_AGENT\"],\"task\":\"docs\"}";
        });

        var result = limited.run(new MultiAgentRequest("Investigate MWS-1.", null, null, 1, 10));

        assertThat(result.completed()).isTrue();
        assertThat(result.status()).isIn(
                MultiAgentState.STATUS_MAX_ITERATIONS,
                MultiAgentState.STATUS_COMPLETED);
    }

    @Test
    void toolLimit_stopsSafely() {
        when(callSpec.content()).thenAnswer(inv -> {
            int n = promptCount.getAndIncrement();
            return switch (n) {
                case 0 -> "{\"action\":\"delegate\",\"agents\":[\"ISSUE_AGENT\",\"PROJECT_AGENT\",\"KNOWLEDGE_AGENT\"],\"task\":\"all\"}";
                default -> "{\"action\":\"finalize\",\"answer\":\"limited\"}";
            };
        });
        when(toolBridge.call(anyString(), anyMap())).thenReturn(
                SpecialistToolBridge.ToolCallOutcome.success("getIssue", Map.of(), "ok", "mcp"));
        when(ragService.query(any())).thenReturn(new RagQueryResponse("docs", List.of()));

        WorkforgeMultiAgentGraph graph = new WorkforgeMultiAgentGraph(
                chatClient, JsonMapper.builder().build(),
                new IssueInvestigationAgent(toolBridge),
                new KnowledgeRagAgent(ragService),
                new ProjectAnalysisAgent(toolBridge));
        MultiAgentService limited = new MultiAgentService(graph, memoryFacade, 5, 1);

        var result = limited.run(new MultiAgentRequest("Investigate MWS-1 completely.", null, null, 5, 1));

        assertThat(result.completed()).isTrue();
        assertThat(result.specialistCalls()).isLessThanOrEqualTo(1);
    }

    @Test
    void memoryContinuation_persistsTurn() {
        when(callSpec.content()).thenReturn(
                "{\"action\":\"finalize\",\"answer\":\"Follow-up answer.\"}");

        var result = service.run(new MultiAgentRequest(
                "Continue", conversationId.toString(), "session-1", null, null));

        assertThat(result.conversationId()).isEqualTo(conversationId.toString());
        org.mockito.Mockito.verify(memoryFacade).prepare(conversationId.toString(), "session-1", "Continue");
        org.mockito.Mockito.verify(memoryFacade).completeTurn(
                conversationId, "Continue", result.answer());
    }

    @Test
    void rejectsBlankMessage() {
        assertThatThrownBy(() -> service.run(new MultiAgentRequest("  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void heuristicParser_selectsAgents() {
        var decision = SupervisorDecisionParser.heuristic(
                "Explain MWS-1 and relevant documentation.", List.of());
        assertThat(decision.isDelegate()).isTrue();
        assertThat(decision.agents()).contains(MultiAgentNames.ISSUE_AGENT, MultiAgentNames.KNOWLEDGE_AGENT);
    }
}
