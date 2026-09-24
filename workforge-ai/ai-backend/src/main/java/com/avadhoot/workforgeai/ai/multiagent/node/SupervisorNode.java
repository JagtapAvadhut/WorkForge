package com.avadhoot.workforgeai.ai.multiagent.node;

import com.avadhoot.workforgeai.ai.multiagent.MultiAgentNames;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentPrompt;
import com.avadhoot.workforgeai.ai.multiagent.MultiAgentState;
import com.avadhoot.workforgeai.ai.multiagent.SupervisorDecisionParser;
import com.avadhoot.workforgeai.ai.multiagent.SupervisorDecisionParser.SupervisorDecision;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Supervisor decides which specialists to run or whether to finalize.
 * Does not query repositories or call tools.
 */
public class SupervisorNode implements NodeAction<MultiAgentState> {

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;

    public SupervisorNode(ChatClient chatClient, JsonMapper jsonMapper) {
        this.chatClient = chatClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public Map<String, Object> apply(MultiAgentState state) {
        int nextIteration = state.iteration() + 1;
        Map<String, Object> updates = new HashMap<>();
        updates.put(MultiAgentState.CURRENT_NODE, MultiAgentNames.SUPERVISOR);
        updates.put(MultiAgentState.ITERATION, nextIteration);

        if (state.completed()) {
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
            return updates;
        }

        if (nextIteration > state.maxIterations()) {
            updates.put(MultiAgentState.STATUS, MultiAgentState.STATUS_MAX_ITERATIONS);
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
            updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                    MultiAgentNames.SUPERVISOR, "limit", Map.of("reason", "max_iterations"))));
            updates.put(MultiAgentState.SUPERVISOR_DECISIONS, List.of(Map.of(
                    "action", "finalize",
                    "reason", "max_iterations")));
            return updates;
        }

        if (state.specialistLimitReached() && !state.specialistResults().isEmpty()) {
            updates.put(MultiAgentState.STATUS, MultiAgentState.STATUS_TOOL_LIMIT);
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
            updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                    MultiAgentNames.SUPERVISOR, "limit", Map.of("reason", "max_specialist_calls"))));
            updates.put(MultiAgentState.SUPERVISOR_DECISIONS, List.of(Map.of(
                    "action", "finalize",
                    "reason", "max_specialist_calls")));
            return updates;
        }

        SupervisorDecision decision = decide(state);
        Map<String, Object> decisionRecord = new HashMap<>();
        decisionRecord.put("action", decision.action());
        decisionRecord.put("agents", decision.agents());
        if (StringUtils.hasText(decision.task())) {
            decisionRecord.put("task", decision.task());
        }
        updates.put(MultiAgentState.SUPERVISOR_DECISIONS, List.of(decisionRecord));

        if (decision.isFinalize()) {
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
            if (StringUtils.hasText(decision.answer())) {
                updates.put(MultiAgentState.FINAL_ANSWER, decision.answer());
            }
            updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                    MultiAgentNames.SUPERVISOR, "finalize", Map.of())));
            return updates;
        }

        List<String> alreadyUsed = state.agentsUsed();
        LinkedHashSet<String> pending = new LinkedHashSet<>();
        for (String agent : decision.agents()) {
            if (!alreadyUsed.contains(agent)) {
                pending.add(agent);
            }
        }
        if (pending.isEmpty()) {
            // All requested agents already ran — finalize
            updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
            updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                    MultiAgentNames.SUPERVISOR, "finalize", Map.of("reason", "agents_already_used"))));
            return updates;
        }

        List<String> pendingList = new ArrayList<>(pending);
        String first = pendingList.getFirst();
        String task = StringUtils.hasText(decision.task()) ? decision.task() : state.userRequest();
        updates.put(MultiAgentState.PENDING_AGENTS, pendingList);
        updates.put(MultiAgentState.CURRENT_TASK, task);
        updates.put(MultiAgentState.NEXT_ROUTE, MultiAgentNames.routeForAgent(first));
        updates.put(MultiAgentState.DELEGATED_TASKS, List.of(Map.of(
                "agents", pendingList,
                "task", task)));
        updates.put(MultiAgentState.STEPS, List.of(MultiAgentState.step(
                MultiAgentNames.SUPERVISOR, "delegate", Map.of(
                        "agents", pendingList,
                        "task", task))));
        return updates;
    }

    private SupervisorDecision decide(MultiAgentState state) {
        try {
            String content = chatClient.prompt()
                    .system(MultiAgentPrompt.supervisorSystem())
                    .user(MultiAgentPrompt.supervisorUser(state))
                    .call()
                    .content();
            return SupervisorDecisionParser.parse(content, jsonMapper)
                    .orElseGet(() -> SupervisorDecisionParser.heuristic(
                            state.userRequest(), state.specialistResults()));
        } catch (Exception ex) {
            return SupervisorDecisionParser.heuristic(state.userRequest(), state.specialistResults());
        }
    }
}
