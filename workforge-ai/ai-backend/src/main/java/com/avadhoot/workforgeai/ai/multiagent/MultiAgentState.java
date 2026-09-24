package com.avadhoot.workforgeai.ai.multiagent;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared multi-agent working state for one invoke(). Not persisted as chain-of-thought.
 */
public class MultiAgentState extends AgentState {

    public static final String USER_REQUEST = "userRequest";
    public static final String CONVERSATION_ID = "conversationId";
    public static final String SUPERVISOR_DECISIONS = "supervisorDecisions";
    public static final String DELEGATED_TASKS = "delegatedTasks";
    public static final String SPECIALIST_RESULTS = "specialistResults";
    public static final String SOURCES = "sources";
    public static final String TOOL_CALLS = "toolCalls";
    public static final String STEPS = "steps";
    public static final String AGENTS_USED = "agentsUsed";
    public static final String PENDING_AGENTS = "pendingAgents";
    public static final String CURRENT_TASK = "currentTask";
    public static final String NEXT_ROUTE = "nextRoute";
    public static final String CURRENT_NODE = "currentNode";
    public static final String ITERATION = "iteration";
    public static final String MAX_ITERATIONS = "maxIterations";
    public static final String SPECIALIST_CALLS = "specialistCalls";
    public static final String MAX_SPECIALIST_CALLS = "maxSpecialistCalls";
    public static final String FINAL_ANSWER = "finalAnswer";
    public static final String COMPLETED = "completed";
    public static final String STATUS = "status";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_MAX_ITERATIONS = "MAX_ITERATIONS";
    public static final String STATUS_TOOL_LIMIT = "TOOL_LIMIT";
    public static final String STATUS_FAILED = "FAILED";

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(USER_REQUEST, Channels.base(() -> "")),
            Map.entry(CONVERSATION_ID, Channels.base(() -> "")),
            Map.entry(SUPERVISOR_DECISIONS, Channels.appender(ArrayList::new)),
            Map.entry(DELEGATED_TASKS, Channels.appender(ArrayList::new)),
            Map.entry(SPECIALIST_RESULTS, Channels.appender(ArrayList::new)),
            Map.entry(SOURCES, Channels.appender(ArrayList::new)),
            Map.entry(TOOL_CALLS, Channels.appender(ArrayList::new)),
            Map.entry(STEPS, Channels.appender(ArrayList::new)),
            Map.entry(AGENTS_USED, Channels.appender(ArrayList::new)),
            Map.entry(PENDING_AGENTS, Channels.base((java.util.function.Supplier<ArrayList<String>>) ArrayList::new)),
            Map.entry(CURRENT_TASK, Channels.base(() -> "")),
            Map.entry(NEXT_ROUTE, Channels.base(() -> MultiAgentNames.ROUTE_FINALIZE)),
            Map.entry(CURRENT_NODE, Channels.base(() -> "")),
            Map.entry(ITERATION, Channels.base(() -> 0)),
            Map.entry(MAX_ITERATIONS, Channels.base(() -> 5)),
            Map.entry(SPECIALIST_CALLS, Channels.base(() -> 0)),
            Map.entry(MAX_SPECIALIST_CALLS, Channels.base(() -> 10)),
            Map.entry(FINAL_ANSWER, Channels.base(() -> "")),
            Map.entry(COMPLETED, Channels.base(() -> Boolean.FALSE)),
            Map.entry(STATUS, Channels.base(() -> STATUS_RUNNING))
    );

    public MultiAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public String userRequest() {
        return value(USER_REQUEST, "");
    }

    public String conversationId() {
        return value(CONVERSATION_ID, "");
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> supervisorDecisions() {
        return value(SUPERVISOR_DECISIONS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> delegatedTasks() {
        return value(DELEGATED_TASKS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> specialistResults() {
        return value(SPECIALIST_RESULTS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> sources() {
        return value(SOURCES, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> toolCalls() {
        return value(TOOL_CALLS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> steps() {
        return value(STEPS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<String> agentsUsed() {
        return value(AGENTS_USED, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<String> pendingAgents() {
        Object raw = value(PENDING_AGENTS).orElse(List.of());
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    out.add(String.valueOf(item));
                }
            }
            return out;
        }
        return List.of();
    }

    public String currentTask() {
        return value(CURRENT_TASK, "");
    }

    public String nextRoute() {
        return value(NEXT_ROUTE, MultiAgentNames.ROUTE_FINALIZE);
    }

    public String currentNode() {
        return value(CURRENT_NODE, "");
    }

    public int iteration() {
        return intVal(ITERATION, 0);
    }

    public int maxIterations() {
        return intVal(MAX_ITERATIONS, 5);
    }

    public int specialistCalls() {
        return intVal(SPECIALIST_CALLS, 0);
    }

    public int maxSpecialistCalls() {
        return intVal(MAX_SPECIALIST_CALLS, 10);
    }

    public String finalAnswer() {
        return value(FINAL_ANSWER, "");
    }

    public boolean completed() {
        return Boolean.TRUE.equals(value(COMPLETED).orElse(Boolean.FALSE));
    }

    public String status() {
        return value(STATUS, STATUS_RUNNING);
    }

    public boolean supervisorLimitReached() {
        return iteration() >= maxIterations();
    }

    public boolean specialistLimitReached() {
        return specialistCalls() >= maxSpecialistCalls();
    }

    private int intVal(String key, int defaultValue) {
        Object raw = value(key).orElse(defaultValue);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    public static Map<String, Object> step(String node, String action, Map<String, Object> details) {
        Map<String, Object> row = new HashMap<>();
        if (details != null) {
            row.putAll(details);
        }
        row.put("node", node);
        row.put("action", action == null ? "" : action);
        return row;
    }
}
