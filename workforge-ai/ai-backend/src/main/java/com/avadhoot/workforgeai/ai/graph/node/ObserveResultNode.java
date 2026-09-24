package com.avadhoot.workforgeai.ai.graph.node;

import com.avadhoot.workforgeai.ai.graph.GraphNodeNames;
import com.avadhoot.workforgeai.ai.graph.WorkforgeAgentState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Records the latest tool observation into conversational context, then loops to decide.
 */
public class ObserveResultNode implements NodeAction<WorkforgeAgentState> {

    @Override
    public Map<String, Object> apply(WorkforgeAgentState state) {
        String tool = state.pendingTool();
        String observation = state.lastObservation();
        String line = StringUtils.hasText(tool)
                ? tool + " => " + (observation == null ? "" : observation)
                : (observation == null ? "" : observation);

        Map<String, Object> step = new HashMap<>();
        step.put("node", GraphNodeNames.OBSERVE_RESULT);
        step.put("action", StringUtils.hasText(tool) ? tool : "observe");

        Map<String, Object> updates = new HashMap<>();
        updates.put(WorkforgeAgentState.CURRENT_NODE, GraphNodeNames.OBSERVE_RESULT);
        updates.put(WorkforgeAgentState.CONTEXT, List.of(line));
        updates.put(WorkforgeAgentState.PENDING_TOOL, "");
        updates.put(WorkforgeAgentState.PENDING_ARGUMENTS, Map.of());
        updates.put(WorkforgeAgentState.STEPS, List.of(step));
        return updates;
    }
}
