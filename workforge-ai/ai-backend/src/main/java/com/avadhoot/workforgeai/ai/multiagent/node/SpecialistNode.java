package com.avadhoot.workforgeai.ai.multiagent.node;

import com.avadhoot.workforgeai.ai.multiagent.IssueInvestigationAgent;
import com.avadhoot.workforgeai.ai.multiagent.KnowledgeRagAgent;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentNames;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentState;
import com.avadhoot.workforgeai.ai.multiagent.ProjectAnalysisAgent;
import com.avadhoot.workforgeai.ai.multiagent.SpecialistResult;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Runs one specialist, appends structured result, and routes to next pending agent or supervisor.
 */
public class SpecialistNode implements NodeAction<MultiAgentState> {

    private final String agentName;
    private final BiFunction<String, String, SpecialistResult> worker;

    public SpecialistNode(String agentName, BiFunction<String, String, SpecialistResult> worker) {
        this.agentName = agentName;
        this.worker = worker;
    }

    public static SpecialistNode issue(IssueInvestigationAgent agent) {
        return new SpecialistNode(MultiAgentNames.ISSUE_AGENT, agent::investigate);
    }

    public static SpecialistNode knowledge(KnowledgeRagAgent agent) {
        return new SpecialistNode(MultiAgentNames.KNOWLEDGE_AGENT, agent::investigate);
    }

    public static SpecialistNode project(ProjectAnalysisAgent agent) {
        return new SpecialistNode(MultiAgentNames.PROJECT_AGENT, agent::analyze);
    }

    @Override
    public Map<String, Object> apply(MultiAgentState state) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(MultiAgentState.CURRENT_NODE, agentName);

        if (state.specialistLimitReached()) {
            updates.put(MultiAgentState.STATUS, MultiAgentState.STATUS_TOOL_LIMIT);
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_SUPERVISOR);
            updates.put(MultiAgentState.PENDING_AGENTS, List.of());
            updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                    agentName, "skipped", Map.of("reason", "max_specialist_calls"))));
            updates.put(MultiAgentState.SPECIALIST_RESULTS, List.of(
                    SpecialistResult.failure(agentName, "Skipped due to specialist call limit").toMap()));
            return updates;
        }

        String task = state.currentTask();
        SpecialistResult result = worker.apply(task, state.userRequest());
        int calls = state.specialistCalls() + 1;

        List<String> pending = new ArrayList<>(state.pendingAgents());
        pending.removeIf(agent -> agentName.equalsIgnoreCase(agent));

        updates.put(MultiAgentState.SPECIALIST_CALLS, calls);
        updates.put(MultiAgentState.SPECIALIST_RESULTS, List.of(result.toMap()));
        updates.put(MultiAgentState.AGENTS_USED, List.of(agentName));
        updates.put(MultiAgentState.PENDING_AGENTS, pending);
        if (!result.sources().isEmpty()) {
            updates.put(MultiAgentState.SOURCES, result.sources());
        }
        if (!result.toolCalls().isEmpty()) {
            updates.put(MultiAgentState.TOOL_CALLS, result.toolCalls());
        }
        updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                agentName,
                result.status(),
                Map.of("summary", result.summary() == null ? "" : result.summary()))));

        if (!pending.isEmpty()) {
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.routeForAgent(pending.getFirst()));
        } else {
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_SUPERVISOR);
        }
        return updates;
    }
}
