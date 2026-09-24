package com.avadhoot.workforgeai.ai.graph.node;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures the user request into graph state and records the first execution step.
 */
public class AnalyzeRequestNode implements NodeAction<WorkforgeAgentState> {

    @Override
    public Map<String, Object> apply(WorkforgeAgentState state) {
        String message = state.userMessage() == null ? "" : state.userMessage().trim();
        Map<String, Object> step = new HashMap<>();
        step.put("node", GraphNodeNames.ANALYZE_REQUEST);
        step.put("action", "analyze");

        Map<String, Object> updates = new HashMap<>();
        updates.put(WorkforgeAgentState.CURRENT_NODE, GraphNodeNames.ANALYZE_REQUEST);
        updates.put(WorkforgeAgentState.USER_MESSAGE, message);
        updates.put(WorkforgeAgentState.CONTEXT, List.of("User request received."));
        updates.put(WorkforgeAgentState.STEPS, List.of(step));
        updates.put(WorkforgeAgentState.STATUS, WorkforgeAgentState.STATUS_RUNNING);
        updates.put(WorkforgeAgentState.COMPLETED, Boolean.FALSE);
        return updates;
    }
}
