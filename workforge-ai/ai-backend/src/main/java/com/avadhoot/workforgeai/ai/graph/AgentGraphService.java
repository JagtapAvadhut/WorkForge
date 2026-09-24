package com.avadhoot.workforgeai.ai.graph;

import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentRequest;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentResponse;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentStep;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the compiled LangGraph4j agent. Orchestration only — no repository access.
 */
@Service
public class AgentGraphService {

    private final CompiledGraph<WorkforgeAgentState> compiledGraph;
    private final ConversationMemoryFacade conversationMemoryFacade;
    private final int defaultMaxIterations;

    public AgentGraphService(
            WorkforgeAgentGraph workforgeAgentGraph,
            ConversationMemoryFacade conversationMemoryFacade,
            @Value("${workforge.ai.graph-agent.max-iterations:5}") int defaultMaxIterations) {
        this.compiledGraph = workforgeAgentGraph.compiled();
        this.conversationMemoryFacade = conversationMemoryFacade;
        this.defaultMaxIterations = Math.max(1, defaultMaxIterations);
    }

    public GraphAgentResponse run(GraphAgentRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }
        var prepared = conversationMemoryFacade.prepare(
                request.conversationId(),
                request.sessionId(),
                request.message());
        String enriched = enrich(request.message().trim(), prepared);
        int maxIterations = resolveMaxIterations(request.maxIterations());

        Map<String, Object> input = new HashMap<>();
        input.put(WorkforgeAgentState.USER_MESSAGE, enriched);
        input.put(WorkforgeAgentState.MAX_ITERATIONS, maxIterations);
        input.put(WorkforgeAgentState.ITERATION, 0);
        input.put(WorkforgeAgentState.COMPLETED, Boolean.FALSE);
        input.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_RUNNING);
        input.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_FINALIZE);

        WorkforgeAgentState finalState = compiledGraph.invoke(input)
                .orElseThrow(() -> new IllegalStateException("Graph produced no final state"));

        conversationMemoryFacade.completeTurn(
                prepared.conversationId(),
                request.message().trim(),
                finalState.finalAnswer());

        return toResponse(finalState, prepared.conversationIdString());
    }

    private static String enrich(String message, ConversationMemoryFacade.PreparedContext prepared) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(prepared.memoryBlock())) {
            sb.append(prepared.memoryBlock()).append("\n\n");
        }
        List<HistoryMessage> history = prepared.recentHistory();
        if (history != null && !history.isEmpty()) {
            sb.append("Recent conversation:\n");
            for (HistoryMessage item : history) {
                sb.append("- ").append(item.role()).append(": ").append(item.content()).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Current user request:\n").append(message);
        return sb.toString().trim();
    }

    private GraphAgentResponse toResponse(WorkforgeAgentState state, String conversationId) {
        List<GraphAgentStep> steps = new ArrayList<>();
        for (Map<String, Object> raw : state.steps()) {
            if (raw == null) {
                continue;
            }
            String node = stringVal(raw.get("node"));
            String action = stringVal(raw.get("action"));
            Map<String, Object> details = new HashMap<>(raw);
            details.remove("node");
            details.remove("action");
            steps.add(new GraphAgentStep(node, action.isBlank() ? null : action, Map.copyOf(details)));
        }

        return new GraphAgentResponse(
                state.finalAnswer(),
                state.completed(),
                state.iteration(),
                state.status(),
                state.currentNode(),
                List.copyOf(steps),
                List.copyOf(state.toolCalls()),
                List.copyOf(state.toolResults()),
                conversationId);
    }

    private int resolveMaxIterations(Integer maxIterations) {
        if (maxIterations == null) {
            return defaultMaxIterations;
        }
        if (maxIterations <= 0) {
            throw new IllegalArgumentException("maxIterations must be greater than 0");
        }
        if (maxIterations > 10) {
            throw new IllegalArgumentException("maxIterations must be at most 10");
        }
        return maxIterations;
    }

    private static String stringVal(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
