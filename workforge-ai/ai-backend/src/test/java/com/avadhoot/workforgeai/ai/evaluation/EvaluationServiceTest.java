package com.avadhoot.workforgeai.ai.evaluation;

import com.avadhoot.workforgeai.ai.agent.AgentService;
import com.avadhoot.workforgeai.ai.agent.dto.AgentResponse;
import com.avadhoot.workforgeai.ai.dto.ChatResponse;
import com.avadhoot.workforgeai.ai.graph.AgentGraphService;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentResponse;
import com.avadhoot.workforgeai.ai.mcp.McpClientGateway;
import com.avadhoot.workforgeai.ai.memory.ConversationService;
import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MessageRecord;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentNames;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentService;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentResponse;
import com.avadhoot.workforgeai.ai.prompt.PromptStrategy;
import com.avadhoot.workforgeai.ai.rag.RagService;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import com.avadhoot.workforgeai.ai.service.AiChatService;
import com.avadhoot.workforgeai.ai.tools.dto.ToolCallInfo;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatResponse;
import com.avadhoot.workforgeai.ai.tools.service.ToolChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EvaluationServiceTest {

    private EvaluationService service;

    @BeforeEach
    void setUp() {
        AiChatService chat = mock(AiChatService.class);
        RagService rag = mock(RagService.class);
        ToolChatService tools = mock(ToolChatService.class);
        AgentService agent = mock(AgentService.class);
        AgentGraphService graph = mock(AgentGraphService.class);
        McpClientGateway mcp = mock(McpClientGateway.class);
        ConversationService conversations = mock(ConversationService.class);
        MultiAgentService multi = mock(MultiAgentService.class);
        SecurityPolicyService security = new SecurityPolicyService(
                "getIssue,searchIssues,getProject", 4000, 10, 5, 10, true);

        when(chat.chat(any())).thenReturn(new ChatResponse("A sprint is a time box.", PromptStrategy.GENERAL));
        when(rag.query(any())).thenReturn(new RagQueryResponse(
                "Statuses FUTURE ACTIVE COMPLETE",
                List.of(new RagSource(UUID.randomUUID(), "doc", Map.of(), 0.9))));
        when(tools.chat(any())).thenReturn(new ToolChatResponse(
                "MWS-1 details",
                List.of(new ToolCallInfo("getIssue", Map.of("issueKey", "MWS-1"), "ok"))));
        when(agent.run(any())).thenReturn(new AgentResponse("sprint answer", List.of(), "completed", 1));
        when(graph.run(any())).thenReturn(new GraphAgentResponse(
                "workflow answer", true, 1, "COMPLETED", "FINALIZE", List.of(), List.of(), List.of(), null));
        when(mcp.status()).thenReturn(new McpClientGateway.McpConnectionStatus(
                true, true, "http://localhost:8091/mcp", "ok"));
        when(mcp.discoverTools()).thenReturn(List.of(
                new McpClientGateway.McpToolDescriptor("getIssue", "d", Map.of()),
                new McpClientGateway.McpToolDescriptor("searchIssues", "d", Map.of()),
                new McpClientGateway.McpToolDescriptor("getProject", "d", Map.of())));
        UUID cid = UUID.randomUUID();
        when(conversations.create(anyString(), anyString())).thenReturn(
                new ConversationRecord(cid, "s", "t", Instant.now(), Instant.now()));
        when(conversations.listMessages(any())).thenReturn(List.of(
                new MessageRecord(UUID.randomUUID(), cid, "user", "What is RAG?", Instant.now()),
                new MessageRecord(UUID.randomUUID(), cid, "assistant", "RAG is...", Instant.now()),
                new MessageRecord(UUID.randomUUID(), cid, "user", "Explain it briefly.", Instant.now()),
                new MessageRecord(UUID.randomUUID(), cid, "assistant", "Simple example...", Instant.now())));
        when(multi.run(any())).thenAnswer(inv -> {
            String msg = ((com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentRequest) inv.getArgument(0)).message();
            List<String> agents = msg.toLowerCase().contains("mws-1")
                    ? List.of(MultiAgentNames.ISSUE_AGENT)
                    : List.of();
            return new MultiAgentResponse("ok", agents, List.of(), 1, agents.size(), "COMPLETED", true,
                    List.of(), List.of(), List.of(), cid.toString());
        });

        service = new EvaluationService(chat, rag, tools, agent, graph, mcp, conversations, multi, security);
    }

    @Test
    void smokeSuite_fastAndPasses() {
        EvaluationRunResult result = service.run("SMOKE");
        assertThat(result.totalCases()).isEqualTo(3);
        assertThat(result.failed()).isZero();
        assertThat(result.passRate()).isEqualTo(100.0);
    }

    @Test
    void asyncRun_returnsRunIdAndCompletes() throws Exception {
        EvaluationRunSnapshot started = service.startAsync("SMOKE");
        assertThat(started.runId()).isNotBlank();
        assertThat(started.status()).isIn("QUEUED", "RUNNING", "COMPLETED");

        EvaluationRunSnapshot done = waitFor(started.runId(), 30_000);
        assertThat(done.status()).isEqualTo("COMPLETED");
        assertThat(done.passedCount()).isEqualTo(3);
        assertThat(done.results()).hasSize(3);
    }

    @Test
    void coreSuite_passes() {
        EvaluationRunResult result = service.run("CORE");
        assertThat(result.failed()).isZero();
        assertThat(result.totalCases()).isGreaterThan(3);
    }

    @Test
    void allSuite_passes() {
        EvaluationRunResult result = service.run("ALL");
        assertThat(result.failed()).isZero();
        assertThat(result.totalCases()).isEqualTo(EvaluationCaseCatalog.all().size());
    }

    @Test
    void multiAgentExpectation() {
        EvaluationRunResult result = service.run("ALL");
        assertThat(result.results().stream()
                .filter(r -> r.caseId().equals("multi-001"))
                .findFirst())
                .get()
                .extracting(EvaluationCaseResult::actualAgents)
                .asList()
                .contains(MultiAgentNames.ISSUE_AGENT);
    }

    @Test
    void deterministicFail_whenChatEmpty() {
        AiChatService chat = mock(AiChatService.class);
        when(chat.chat(any())).thenReturn(new ChatResponse(" ", PromptStrategy.GENERAL));
        EvaluationService failing = new EvaluationService(
                chat,
                mock(RagService.class),
                mock(ToolChatService.class),
                mock(AgentService.class),
                mock(AgentGraphService.class),
                mock(McpClientGateway.class),
                mock(ConversationService.class),
                mock(MultiAgentService.class),
                new SecurityPolicyService("getIssue,searchIssues,getProject", 4000, 10, 5, 10, true));

        EvaluationRunResult result = failing.run("CHAT");
        // CHAT suite filter by category — catalog uses inSuite; "CHAT" as suite filters by suite tags
        // Use CORE which includes chat-001
        result = failing.run("CORE");
        assertThat(result.results().stream().anyMatch(r -> "chat-001".equals(r.caseId()) && !r.passed())).isTrue();
    }

    private EvaluationRunSnapshot waitFor(String runId, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            EvaluationRunSnapshot snap = service.getRun(runId);
            if ("COMPLETED".equals(snap.status()) || "FAILED".equals(snap.status())) {
                return snap;
            }
            Thread.sleep(100);
        }
        return service.getRun(runId);
    }
}
