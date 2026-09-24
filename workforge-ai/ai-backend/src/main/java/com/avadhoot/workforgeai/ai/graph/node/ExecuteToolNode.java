package com.avadhoot.workforgeai.ai.graph.node;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import com.avadhoot.workforgeai.ai.tools.RecordingToolCallback;
import com.avadhoot.workforgeai.ai.tools.WorkforgeTools;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes one Phase 6 read-only tool selected by {@link DecideActionNode}.
 * Goes through ToolCallback → WorkforgeTools → application services (not repositories).
 */
public class ExecuteToolNode implements NodeAction<WorkforgeAgentState> {

    private final Map<String, ToolCallback> callbacksByName;
    private final JsonMapper jsonMapper;

    public ExecuteToolNode(WorkforgeTools workforgeTools, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(workforgeTools)
                .build()
                .getToolCallbacks();
        this.callbacksByName = new LinkedHashMap<>();
        for (ToolCallback callback : callbacks) {
            callbacksByName.put(callback.getToolDefinition().name(), callback);
        }
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

        Map<String, Object> step = new HashMap<>();
        step.put("node", GraphNodeNames.EXECUTE_TOOL);
        step.put("action", tool == null ? "" : tool);

        if (!StringUtils.hasText(tool)) {
            String observation = "No tool selected";
            updates.put(WorkforgeAgentState.LAST_OBSERVATION, observation);
            updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_TOOL_FAILURE);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(Map.of(
                    "tool", "",
                    "result", observation,
                    "ok", false)));
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        }

        ToolCallback callback = callbacksByName.get(tool);
        if (callback == null) {
            String observation = "Unknown tool: " + tool;
            updates.put(WorkforgeAgentState.LAST_OBSERVATION, observation);
            updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_TOOL_FAILURE);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(Map.of(
                    "tool", tool,
                    "result", observation,
                    "ok", false)));
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        }

        try {
            String argsJson = jsonMapper.writeValueAsString(args == null ? Map.of() : args);
            List<RecordingToolCallback.RecordedToolCall> sink = RecordingToolCallback.newSink();
            String observation = new RecordingToolCallback(callback, sink, jsonMapper).call(argsJson);
            String summarized = summarize(observation);

            updates.put(WorkforgeAgentState.LAST_OBSERVATION, summarized);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            Map<String, Object> resultRecord = new HashMap<>();
            resultRecord.put("tool", tool);
            resultRecord.put("result", summarized);
            resultRecord.put("ok", true);
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(resultRecord));
            updates.put(WorkforgeAgentState.STEPS, List.of(step));
            return updates;
        } catch (Exception ex) {
            String observation = "Tool failure: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            updates.put(WorkforgeAgentState.LAST_OBSERVATION, observation);
            updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_TOOL_FAILURE);
            updates.put(WorkforgeAgentState.TOOL_CALLS, List.of(callRecord));
            updates.put(WorkforgeAgentState.TOOL_RESULTS, List.of(Map.of(
                    "tool", tool,
                    "result", observation,
                    "ok", false)));
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
