package com.avadhoot.workforgeai.ai.mcp.node;

import com.avadhoot.workforgeai.ai.agent.AgentDecisionParser;
import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import com.avadhoot.workforgeai.ai.mcp.McpPrompt;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decide node for MCP agent — same shape as Phase 8, MCP-oriented prompt.
 */
public class McpDecideActionNode implements NodeAction<WorkforgeAgentState> {

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    public McpDecideActionNode(ChatClient chatClient, JsonMapper jsonMapper) {
        this.chatClient = chatClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Map<String, Object> apply(WorkforgeAgentState state) {
        int nextIteration = state.iteration() + 1;
        Map<String, Object> updates = new HashMap<>();
        updates.put(WorkforgeAgentState.CURRENT_NODE, GraphNodeNames.DECIDE_ACTION);
        updates.put(WorkforgeAgentState.ITERATION, nextIteration);

        if (nextIteration > state.maxIterations()) {
            updates.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_FINALIZE);
            updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_MAX_ITERATIONS);
            updates.put(WorkforgeAgentState.PENDING_TOOL, "");
            updates.put(WorkforgeAgentState.PENDING_ARGUMENTS, Map.of());
            updates.put(WorkforgeAgentState.STEPS, List.of(Map.of(
                    "node", GraphNodeNames.DECIDE_ACTION,
                    "action", "max_iterations")));
            return updates;
        }

        String content = chatClient.prompt()
                .system(McpPrompt.DECIDE_SYSTEM)
                .user(buildUserPrompt(state))
                .call()
                .content();

        AgentDecisionParser.AgentDecision decision = AgentDecisionParser.parse(content, jsonMapper).orElse(null);
        if (decision == null && StringUtils.hasText(content) && !looksLikeJson(content)) {
            updates.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_FINALIZE);
            updates.put(WorkforgeAgentState.FINAL_ANSWER, content.trim());
            updates.put(WorkforgeAgentState.PENDING_TOOL, "");
            updates.put(WorkforgeAgentState.PENDING_ARGUMENTS, Map.of());
            updates.put(WorkforgeAgentState.STEPS, List.of(Map.of(
                    "node", GraphNodeNames.DECIDE_ACTION,
                    "action", "final")));
            return updates;
        }

        if (decision != null && decision.isTool()) {
            updates.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_TOOL);
            updates.put(WorkforgeAgentState.PENDING_TOOL, decision.tool());
            updates.put(WorkforgeAgentState.PENDING_ARGUMENTS,
                    decision.arguments() == null ? Map.of() : new HashMap<>(decision.arguments()));
            Map<String, Object> step = new HashMap<>();
            step.put("node", GraphNodeNames.DECIDE_ACTION);
            step.put("action", decision.tool());
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        }

        if (decision != null && decision.isFinal()) {
            updates.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_FINALIZE);
            updates.put(WorkforgeAgentState.FINAL_ANSWER, decision.answer());
            updates.put(WorkforgeAgentState.PENDING_TOOL, "");
            updates.put(WorkforgeAgentState.PENDING_ARGUMENTS, Map.of());
            updates.put(WorkforgeAgentState.STEPS, List.of(Map.of(
                    "node", GraphNodeNames.DECIDE_ACTION,
                    "action", "final")));
            return updates;
        }

        updates.put(WorkforgeAgentState.NEXT_ROUTE, WorkforgeAgentState.ROUTE_FINALIZE);
        updates.put(WorkforgeAgentState.PENDING_TOOL, "");
        updates.put(WorkforgeAgentState.PENDING_ARGUMENTS, Map.of());
        updates.put(WorkforgeAgentState.STEPS, List.of(Map.of(
                "node", GraphNodeNames.DECIDE_ACTION,
                "action", "invalid_decision")));
        return updates;
    }

    private static String buildUserPrompt(WorkforgeAgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("User request:\n").append(state.userMessage()).append("\n\n");
        sb.append("Iteration: ").append(state.iteration() + 1).append(" of ")
                .append(state.maxIterations()).append("\n\n");
        List<String> context = state.context();
        if (context == null || context.isEmpty()) {
            sb.append("Observations so far: (none)\n\n");
        } else {
            sb.append("Observations / context:\n");
            for (String line : context) {
                sb.append("- ").append(line).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Decide the next action as a single JSON object.");
        return sb.toString();
    }

    private static boolean looksLikeJson(String content) {
        String trimmed = content == null ? "" : content.trim();
        return trimmed.startsWith("{") || trimmed.startsWith("[");
    }
}
