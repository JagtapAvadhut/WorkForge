package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentRequest;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentResponse;
import com.avadhoot.workforgeai.ai.multiagent.dto.MultiAgentStep;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class MultiAgentService {

    private final CompiledGraph<MultiAgentState> compiledGraph;
    private final ConversationMemoryFacade conversationMemoryFacade;
    private final int defaultMaxIterations;
    private final int defaultMaxSpecialistCalls;

    public MultiAgentService(
            WorkforgeMultiAgentGraph workforgeMultiAgentGraph,
            ConversationMemoryFacade conversationMemoryFacade,
            @Value("${workforge.ai.multi-agent.max-iterations:5}") int defaultMaxIterations,
            @Value("${workforge.ai.multi-agent.max-specialist-calls:10}") int defaultMaxSpecialistCalls) {
        this.compiledGraph = workforgeMultiAgentGraph.compiled();
        this.conversationMemoryFacade = conversationMemoryFacade;
        this.defaultMaxIterations = Math.max(1, defaultMaxIterations);
        this.defaultMaxSpecialistCalls = Math.max(1, defaultMaxSpecialistCalls);
    }

    public MultiAgentResponse run(MultiAgentRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }

        var prepared = conversationMemoryFacade.prepare(
                request.conversationId(),
                request.sessionId(),
                request.message());
        String enriched = enrich(request.message().trim(), prepared);

        Map<String, Object> input = new HashMap<>();
        input.put(MultiAgentState.USER_REQUEST, enriched);
        input.put(MultiAgentState.CONVERSATION_ID, prepared.conversationIdString());
        input.put(MultiAgentState.MAX_ITERATIONS, resolveMaxIterations(request.maxIterations()));
        input.put(MultiAgentState.MAX_SPECIALIST_CALLS, resolveMaxSpecialistCalls(request.maxSpecialistCalls()));
        input.put(MultiAgentState.ITERATION, 0);
        input.put(MultiAgentState.SPECIALIST_CALLS, 0);
        input.put(MultiAgentState.COMPLETED, Boolean.FALSE);
        input.put(MultiAgentState.STATUS, MultiAgentState.STATUS_RUNNING);
        input.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
        input.put(MultiAgentState.PENDING_AGENTS, new ArrayList<String>());

        MultiAgentState finalState = compiledGraph.invoke(input)
                .orElseThrow(() -> new IllegalStateException("Multi-agent graph produced no final state"));

        conversationMemoryFacade.completeTurn(
                prepared.conversationId(),
                request.message().trim(),
                finalState.finalAnswer());

        return toResponse(finalState, prepared.conversationIdString());
    }

    private MultiAgentResponse toResponse(MultiAgentState state, String conversationId) {
        List<MultiAgentStep> steps = new ArrayList<>();
        for (Map<String, Object> raw : state.steps()) {
            if (raw == null) {
                continue;
            }
            String node = String.valueOf(raw.getOrDefault("node", ""));
            Object actionObj = raw.get("action");
            String action = actionObj == null ? null : String.valueOf(actionObj);
            Map<String, Object> details = new HashMap<>(raw);
            details.remove("node");
            details.remove("action");
            steps.add(new MultiAgentStep(node, action, Map.copyOf(details)));
        }

        LinkedHashSet<String> agents = new LinkedHashSet<>();
        for (String agent : state.agentsUsed()) {
            if (StringUtils.hasText(agent)) {
                agents.add(agent);
            }
        }

        return new MultiAgentResponse(
                state.finalAnswer(),
                List.copyOf(agents),
                List.copyOf(steps),
                state.iteration(),
                state.specialistCalls(),
                state.status(),
                state.completed(),
                List.copyOf(state.specialistResults()),
                List.copyOf(state.sources()),
                List.copyOf(state.toolCalls()),
                conversationId);
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

    private int resolveMaxIterations(Integer value) {
        if (value == null) {
            return defaultMaxIterations;
        }
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("maxIterations must be between 1 and 5");
        }
        return value;
    }

    private int resolveMaxSpecialistCalls(Integer value) {
        if (value == null) {
            return defaultMaxSpecialistCalls;
        }
        if (value < 1 || value > 20) {
            throw new IllegalArgumentException("maxSpecialistCalls must be between 1 and 20");
        }
        return value;
    }
}
