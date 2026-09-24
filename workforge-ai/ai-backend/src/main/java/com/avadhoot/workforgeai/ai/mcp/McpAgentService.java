package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import com.avadhoot.workforgeai.ai.graph.dto.GraphAgentStep;
import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentRequest;
import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentResponse;
import com.avadhoot.workforgeai.ai.memory.ConversationMemoryFacade;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class McpAgentService {

    private final CompiledGraph<WorkforgeAgentState> compiledGraph;
    private final McpClientGateway mcpClientGateway;
    private final ConversationMemoryFacade conversationMemoryFacade;
    private final int defaultMaxIterations;

    public McpAgentService(
            WorkforgeMcpAgentGraph workforgeMcpAgentGraph,
            McpClientGateway mcpClientGateway,
            ConversationMemoryFacade conversationMemoryFacade,
            @Value("${workforge.ai.graph-agent.max-iterations:5}") int defaultMaxIterations) {
        this.compiledGraph = workforgeMcpAgentGraph.compiled();
        this.mcpClientGateway = mcpClientGateway;
        this.conversationMemoryFacade = conversationMemoryFacade;
        this.defaultMaxIterations = Math.max(1, defaultMaxIterations);
    }

    public McpAgentResponse run(McpAgentRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }
        if (!mcpClientGateway.status().serverUp()) {
            throw new McpUnavailableException("MCP server is unavailable; cannot run mcp-agent");
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
                .orElseThrow(() -> new IllegalStateException("MCP agent graph produced no final state"));

        conversationMemoryFacade.completeTurn(
                prepared.conversationId(),
                request.message().trim(),
                finalState.finalAnswer());

        List<GraphAgentStep> steps = new ArrayList<>();
        for (Map<String, Object> raw : finalState.steps()) {
            if (raw == null) {
                continue;
            }
            String node = String.valueOf(raw.getOrDefault("node", ""));
            Object actionObj = raw.get("action");
            String action = actionObj == null ? null : String.valueOf(actionObj);
            Map<String, Object> details = new HashMap<>(raw);
            details.remove("node");
            details.remove("action");
            steps.add(new GraphAgentStep(node, action, Map.copyOf(details)));
        }

        List<Map<String, Object>> toolsUsed = finalState.toolCalls().stream()
                .map(call -> {
                    Map<String, Object> row = new HashMap<>(call);
                    row.putIfAbsent("via", "mcp");
                    return row;
                })
                .toList();

        return new McpAgentResponse(
                finalState.finalAnswer(),
                finalState.completed(),
                finalState.iteration(),
                finalState.status(),
                finalState.currentNode(),
                List.copyOf(steps),
                List.copyOf(toolsUsed),
                prepared.conversationIdString());
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

    private int resolveMaxIterations(Integer maxIterations) {
        if (maxIterations == null) {
            return defaultMaxIterations;
        }
        if (maxIterations <= 0 || maxIterations > 10) {
            throw new IllegalArgumentException("maxIterations must be between 1 and 10");
        }
        return maxIterations;
    }
}
