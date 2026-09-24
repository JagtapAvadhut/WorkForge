package com.avadhoot.workforgeai.ai.evaluation;

import com.avadhoot.workforgeai.ai.agent.AgentService;
import com.avadhoot.workforgeai.ai.agent.dto.AgentRequest;
import com.avadhoot.workforgeai.ai.agent.dto.AgentResponse;
import com.avadhoot.workforgeai.ai.dto.ChatRequest;
import com.avadhoot.workforgeai.ai.dto.ChatResponse;
import com.avadhoot.workforgeai.ai.graph.AgentGraphService;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentRequest;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentResponse;
import com.avadhoot.workforgeai.ai.mcp.McpClientGateway;
import com.avadhoot.workforgeai.ai.memory.ConversationService;
import com.avadhoot.workforgeai.ai.memory.model.MessageRecord;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentService;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentRequest;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentResponse;
import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.rag.RagService;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryRequest;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.security.SecurityCheckResult;
import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import com.avadhoot.workforgeai.ai.security.SecurityStatus;
import com.avadhoot.workforgeai.ai.service.AiChatService;
import com.avadhoot.workforgeai.ai.tools.dto.ToolCallInfo;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatRequest;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatResponse;
import com.avadhoot.workforgeai.ai.tools.service.ToolChatService;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Async evaluation runner with per-case timeouts. Preserves partial results.
 */
@Service
public class EvaluationService {

    private final AiChatService aiChatService;
    private final RagService ragService;
    private final ToolChatService toolChatService;
    private final AgentService agentService;
    private final AgentGraphService agentGraphService;
    private final McpClientGateway mcpClientGateway;
    private final ConversationService conversationService;
    private final MultiAgentService multiAgentService;
    private final SecurityPolicyService securityPolicyService;

    private final Map<String, EvaluationRunState> runs = new ConcurrentHashMap<>();
    private final ExecutorService suiteExecutor = Executors.newFixedThreadPool(2);
    private final ExecutorService caseExecutor = Executors.newCachedThreadPool();

    public EvaluationService(
            AiChatService aiChatService,
            RagService ragService,
            ToolChatService toolChatService,
            AgentService agentService,
            AgentGraphService agentGraphService,
            McpClientGateway mcpClientGateway,
            ConversationService conversationService,
            MultiAgentService multiAgentService,
            SecurityPolicyService securityPolicyService) {
        this.aiChatService = aiChatService;
        this.ragService = ragService;
        this.toolChatService = toolChatService;
        this.agentService = agentService;
        this.agentGraphService = agentGraphService;
        this.mcpClientGateway = mcpClientGateway;
        this.conversationService = conversationService;
        this.multiAgentService = multiAgentService;
        this.securityPolicyService = securityPolicyService;
    }

    @PreDestroy
    void shutdown() {
        suiteExecutor.shutdownNow();
        caseExecutor.shutdownNow();
    }

    /**
     * Starts an async evaluation job and returns immediately.
     */
    public EvaluationRunSnapshot startAsync(String suite) {
        String normalized = normalizeSuite(suite);
        List<EvaluationCase> cases = EvaluationCaseCatalog.bySuite(normalized);
        if (cases.isEmpty()) {
            throw new IllegalArgumentException("Unknown evaluation suite: " + suite);
        }
        String runId = "eval-" + UUID.randomUUID();
        EvaluationRunState state = new EvaluationRunState(runId, normalized, cases.size());
        runs.put(runId, state);
        suiteExecutor.submit(() -> executeSuite(state, cases));
        return state.snapshot();
    }

    public EvaluationRunSnapshot getRun(String runId) {
        EvaluationRunState state = runs.get(runId);
        if (state == null) {
            throw new IllegalArgumentException("Evaluation run not found: " + runId);
        }
        return state.snapshot();
    }

    /**
     * Synchronous helper for unit tests / smoke scripts that need immediate results.
     */
    public EvaluationRunResult run(String suite) {
        EvaluationRunSnapshot started = startAsync(suite);
        long deadline = System.currentTimeMillis() + 15 * 60_000L;
        while (System.currentTimeMillis() < deadline) {
            EvaluationRunSnapshot snap = getRun(started.runId());
            if ("COMPLETED".equals(snap.status()) || "FAILED".equals(snap.status()) || "CANCELLED".equals(snap.status())) {
                return new EvaluationRunResult(
                        snap.totalCases(),
                        snap.passedCount(),
                        snap.failedCount(),
                        snap.passRate(),
                        snap.averageLatencyMs(),
                        snap.results());
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        EvaluationRunSnapshot snap = getRun(started.runId());
        return new EvaluationRunResult(
                snap.totalCases(),
                snap.passedCount(),
                snap.failedCount(),
                snap.passRate(),
                snap.averageLatencyMs(),
                snap.results());
    }

    private void executeSuite(EvaluationRunState state, List<EvaluationCase> cases) {
        state.setStatus(EvaluationRunState.Status.RUNNING);
        try {
            for (EvaluationCase evaluationCase : cases) {
                state.setCurrent(evaluationCase.id(), evaluationCase.category());
                EvaluationCaseResult result = runOneWithTimeout(evaluationCase);
                state.addResult(result);
            }
            state.setStatus(EvaluationRunState.Status.COMPLETED);
        } catch (Exception ex) {
            state.setError(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            state.setStatus(EvaluationRunState.Status.FAILED);
        } finally {
            state.setCurrent(null, null);
            state.setCompletedAt(Instant.now());
        }
    }

    private EvaluationCaseResult runOneWithTimeout(EvaluationCase evaluationCase) {
        Future<EvaluationCaseResult> future = caseExecutor.submit(() -> runOne(evaluationCase));
        try {
            return future.get(evaluationCase.timeoutMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            return fail(
                    evaluationCase,
                    System.currentTimeMillis(),
                    "Case timed out after " + evaluationCase.timeoutMs() + "ms",
                    List.of(),
                    List.of(),
                    "",
                    null);
        } catch (Exception ex) {
            future.cancel(true);
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            return fail(
                    evaluationCase,
                    System.currentTimeMillis(),
                    cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage(),
                    List.of(),
                    List.of(),
                    "",
                    null);
        }
    }

    private EvaluationCaseResult runOne(EvaluationCase evaluationCase) {
        long start = System.currentTimeMillis();
        try {
            return switch (evaluationCase.checkType()) {
                case "RESPONSE_EXISTS" -> checkChat(evaluationCase, start);
                case "RAG_SOURCES_OR_NO_CONTEXT" -> checkRag(evaluationCase, start);
                case "TOOL_OR_RESPONSE" -> checkTool(evaluationCase, start);
                case "AGENT_COMPLETED", "AGENT_ITERATION_LIMIT" -> checkAgent(evaluationCase, start);
                case "GRAPH_COMPLETED" -> checkGraph(evaluationCase, start);
                case "MCP_TOOLS_DISCOVERED" -> checkMcp(evaluationCase, start);
                case "MEMORY_CONTINUATION" -> checkMemory(evaluationCase, start);
                case "MULTI_AGENT_AGENTS", "MULTI_AGENT_COMPLETED" -> checkMultiAgent(evaluationCase, start);
                case "SECURITY_SAFE" -> checkSecuritySafe(evaluationCase, start);
                case "SECURITY_SUSPICIOUS" -> checkSecuritySuspicious(evaluationCase, start);
                default -> fail(evaluationCase, start, "Unknown checkType", List.of(), List.of(), "", null);
            };
        } catch (Exception ex) {
            return fail(
                    evaluationCase,
                    start,
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(),
                    List.of(),
                    List.of(),
                    "",
                    currentTraceId());
        }
    }

    private EvaluationCaseResult checkSecuritySafe(EvaluationCase c, long start) {
        SecurityCheckResult check = securityPolicyService.checkInput(c.input());
        boolean pass = check.status() == SecurityStatus.SAFE && check.allowed();
        return result(c, start, pass, check.status().name() + ": " + check.reason(),
                List.of(), List.of(), pass ? null : "Expected SAFE", null);
    }

    private EvaluationCaseResult checkSecuritySuspicious(EvaluationCase c, long start) {
        SecurityCheckResult check = securityPolicyService.checkInput(c.input());
        boolean pass = check.status() == SecurityStatus.SUSPICIOUS || check.status() == SecurityStatus.BLOCKED;
        return result(c, start, pass, check.status().name() + ": " + check.reason(),
                List.of(), List.of(), pass ? null : "Expected SUSPICIOUS/BLOCKED", null);
    }

    private EvaluationCaseResult checkChat(EvaluationCase c, long start) {
        ChatResponse response = aiChatService.chat(new ChatRequest(c.input()));
        boolean pass = StringUtils.hasText(response.response());
        return result(c, start, pass, response.response(), List.of(), List.of(),
                pass ? null : "Empty chat response", currentTraceId());
    }

    private EvaluationCaseResult checkRag(EvaluationCase c, long start) {
        RagQueryResponse response = ragService.query(new RagQueryRequest(c.input(), 3));
        boolean hasAnswer = StringUtils.hasText(response.answer());
        boolean pass = hasAnswer;
        return result(c, start, pass, response.answer(), List.of(), List.of(),
                pass ? null : "RAG returned empty answer", currentTraceId());
    }

    private EvaluationCaseResult checkTool(EvaluationCase c, long start) {
        ToolChatResponse response = toolChatService.chat(new ToolChatRequest(c.input()));
        List<String> actualTools = response.toolCalls() == null
                ? List.of()
                : response.toolCalls().stream().map(ToolCallInfo::tool).toList();
        boolean pass = StringUtils.hasText(response.response());
        if (!c.expectedTools().isEmpty() && !actualTools.isEmpty()) {
            pass = actualTools.stream().anyMatch(tool ->
                    c.expectedTools().stream().anyMatch(expected -> expected.equalsIgnoreCase(tool)));
        }
        return result(c, start, pass, response.response(), actualTools, List.of(),
                pass ? null : "Tool expectation not met or empty response", currentTraceId());
    }

    private EvaluationCaseResult checkAgent(EvaluationCase c, long start) {
        AgentResponse response = agentService.run(new AgentRequest(c.input(), 3));
        List<String> actualTools = response.steps() == null
                ? List.of()
                : response.steps().stream()
                        .map(step -> step.tool())
                        .filter(StringUtils::hasText)
                        .toList();
        boolean pass = StringUtils.hasText(response.answer())
                && response.iterations() <= 5
                && ("completed".equalsIgnoreCase(response.stopReason())
                || "max_steps".equalsIgnoreCase(response.stopReason()));
        return result(c, start, pass, response.answer(), actualTools, List.of(),
                pass ? null : "Agent did not complete safely", currentTraceId());
    }

    private EvaluationCaseResult checkGraph(EvaluationCase c, long start) {
        GraphAgentResponse response = agentGraphService.run(new GraphAgentRequest(c.input(), 3));
        List<String> actualTools = response.toolCalls() == null
                ? List.of()
                : response.toolCalls().stream()
                        .map(call -> String.valueOf(call.getOrDefault("tool", "")))
                        .filter(StringUtils::hasText)
                        .toList();
        boolean pass = response.completed() && StringUtils.hasText(response.response());
        return result(c, start, pass, response.response(), actualTools, List.of(),
                pass ? null : "Graph did not complete", currentTraceId());
    }

    private EvaluationCaseResult checkMcp(EvaluationCase c, long start) {
        var tools = mcpClientGateway.discoverTools();
        List<String> actualTools = tools.stream().map(McpClientGateway.McpToolDescriptor::name).toList();
        boolean pass = !actualTools.isEmpty()
                && c.expectedTools().stream().allMatch(expected ->
                actualTools.stream().anyMatch(actual -> actual.equalsIgnoreCase(expected)));
        if (!mcpClientGateway.status().serverUp()) {
            pass = false;
        }
        return result(c, start, pass, "discovered=" + actualTools, actualTools, List.of(),
                pass ? null : "MCP tools missing or server down", null);
    }

    private EvaluationCaseResult checkMemory(EvaluationCase c, long start) {
        String session = "eval-memory-" + System.nanoTime();
        var conversation = conversationService.create(session, "eval");
        ChatResponse first = aiChatService.chat(new ChatRequest(
                c.input(), null, List.of(), conversation.id().toString(), session));
        ChatResponse second = aiChatService.chat(new ChatRequest(
                "Explain it briefly.", null, List.of(), conversation.id().toString(), session));
        List<MessageRecord> messages = conversationService.listMessages(conversation.id());
        boolean pass = messages.size() >= 4
                && StringUtils.hasText(first.response())
                && StringUtils.hasText(second.response());
        return result(c, start, pass, second.response(), List.of(), List.of(),
                pass ? null : "Memory continuation did not persist enough messages", currentTraceId());
    }

    private EvaluationCaseResult checkMultiAgent(EvaluationCase c, long start) {
        MultiAgentResponse response = multiAgentService.run(new MultiAgentRequest(c.input()));
        List<String> actualAgents = response.agentsUsed() == null ? List.of() : response.agentsUsed();
        boolean pass = response.completed() && StringUtils.hasText(response.answer());
        if (!c.expectedAgents().isEmpty()) {
            pass = pass && c.expectedAgents().stream().allMatch(expected ->
                    actualAgents.stream().anyMatch(actual -> actual.equalsIgnoreCase(expected)));
        }
        if ("MULTI_AGENT_COMPLETED".equals(c.checkType()) && c.input().toLowerCase(Locale.ROOT).contains("sprint")) {
            pass = response.completed() && StringUtils.hasText(response.answer());
        }
        return result(c, start, pass, response.answer(), List.of(), actualAgents,
                pass ? null : "Multi-agent expectation failed", currentTraceId());
    }

    private static String normalizeSuite(String suite) {
        if (suite == null || suite.isBlank()) {
            return "ALL";
        }
        return suite.trim().toUpperCase(Locale.ROOT);
    }

    private static String currentTraceId() {
        AiExecutionContext ctx = AiExecutionContext.current();
        return ctx == null ? null : ctx.traceId();
    }

    private static EvaluationCaseResult result(
            EvaluationCase c,
            long start,
            boolean pass,
            String answer,
            List<String> actualTools,
            List<String> actualAgents,
            String error,
            String traceId) {
        return new EvaluationCaseResult(
                c.id(),
                c.category(),
                pass,
                c.expected(),
                answer,
                c.expectedTools(),
                actualTools,
                c.expectedAgents(),
                actualAgents,
                System.currentTimeMillis() - start,
                error,
                traceId);
    }

    private static EvaluationCaseResult fail(
            EvaluationCase c,
            long start,
            String error,
            List<String> actualTools,
            List<String> actualAgents,
            String answer,
            String traceId) {
        return new EvaluationCaseResult(
                c.id(),
                c.category(),
                false,
                c.expected(),
                answer,
                c.expectedTools(),
                actualTools,
                c.expectedAgents(),
                actualAgents,
                Math.max(0, System.currentTimeMillis() - start),
                error,
                traceId);
    }
}
