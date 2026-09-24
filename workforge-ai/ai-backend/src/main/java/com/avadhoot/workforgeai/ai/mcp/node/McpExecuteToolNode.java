package com.avadhoot.workforgeai.ai.mcp.node;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import com.avadhoot.workforgeai.ai.mcp.McpClientGateway;
import com.avadhoot.workforgeai.ai.mcp.McpToolException;
import com.avadhoot.workforgeai.ai.mcp.McpUnavailableException;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a tool via MCP client → MCP server (never local Java tools).
 */
public class McpExecuteToolNode implements NodeAction<WorkforgeAgentState> {

    private final McpClientGateway mcpClientGateway;

    public McpExecuteToolNode(McpClientGateway mcpClientGateway) {
        this.mcpClientGateway = mcpClientGateway;
    }

    @Override
    public Map<String, Object> apply(WorkforgeAgentState state) {
        String tool = state.pendingTool();
        Map<String, Object> args = state.pendingArguments();
        Map<String, Object> updates = new HashMap<>();
        updates.put(WorkforgeAgentState.CURRENT_NODE, GraphNodeNames.EXECUTE_TOOL);

        Map<String, Object> callRecord = new HashMap<>();
        callRecord.put("tool", tool);
        callRecord.put("arguments", args);
        callRecord.put("via", "mcp");

        Map<String, Object> step = new HashMap<>();
        step.put("node", GraphNodeNames.EXECUTE_TOOL);
        step.put("action", tool == null ? "" : tool);

        if (!StringUtils.hasText(tool)) {
            String observation = "No MCP tool selected";
            updates.put(WorkforgeAgentState.LAST_OBSERVATION, observation);
            updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_TOOL_FAILURE);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(Map.of(
                    "tool", "", "result", observation, "ok", false, "via", "mcp")));
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        }

        try {
            String observation = summarize(mcpClientGateway.executeTool(tool, args));
            updates.put(WorkforgeAgentState.LAST_OBSERVATION, observation);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            Map<String, Object> resultRecord = new HashMap<>();
            resultRecord.put("tool", tool);
            resultRecord.put("result", observation);
            resultRecord.put("ok", true);
            resultRecord.put("via", "mcp");
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(resultRecord));
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        } catch (McpUnavailableException | McpToolException ex) {
            String observation = "MCP tool failure: " + ex.getMessage();
            updates.put(WorkforgeAgentState.LAST_OBSERVATION, observation);
            updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_TOOL_FAILURE);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(Map.of(
                    "tool", tool,
                    "result", observation,
                    "ok", false,
                    "via", "mcp")));
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        }
    }

    private static String summarize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 500) {
            return trimmed;
        }
        return trimmed.substring(0, 497) + "...";
    }
}
