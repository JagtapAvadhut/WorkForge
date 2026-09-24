package com.avadhoot.workforgeai.ai.graph;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Typed LangGraph4j state passed between WorkForge AI graph nodes.
 * Not a global singleton — each invoke() creates a fresh state map.
 */
public class WorkforgeAgentState extends AgentState {

    public static final String USER_MESSAGE = "userMessage";
    public static final String CONTEXT = "context";
    public static final String TOOL_CALLS = "toolCalls";
    public static final String TOOL_RESULTS = "toolResults";
    public static final String STEPS = "steps";
    public static final String CURRENT_NODE = "currentNode";
    public static final String ITERATION = "iteration";
    public static final String MAX_ITERATIONS = "maxIterations";
    public static final String NEXT_ROUTE = "nextRoute";
    public static final String PENDING_TOOL = "pendingTool";
    public static final String PENDING_ARGUMENTS = "pendingArguments";
    public static final String LAST_OBSERVATION = "lastObservation";
    public static final String FINAL_ANSWER = "finalAnswer";
    public static final String COMPLETED = "completed";
    public static final String STATUS = "status";

    public static final String ROUTE_TOOL = "tool";
    public static final String ROUTE_FINALIZE = "finalize";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_MAX_ITERATIONS = "MAX_ITERATIONS";
    public static final String STATUS_TOOL_FAILURE = "TOOL_FAILURE";

    public static final Map<String, Channel<?>> SCHEMA = Map.ofEntries(
            Map.entry(USER_MESSAGE, Channels.base(() -> "")),
            Map.entry(CONTEXT, Channels.appender(ArrayList::new)),
            Map.entry(TOOL_CALLS, Channels.appender(ArrayList::new)),
            Map.entry(TOOL_RESULTS, Channels.appender(ArrayList::new)),
            Map.entry(STEPS, Channels.appender(ArrayList::new)),
            Map.entry(CURRENT_NODE, Channels.base(() -> "")),
            Map.entry(ITERATION, Channels.base(() -> 0)),
            Map.entry(MAX_ITERATIONS, Channels.base(() -> 5)),
            Map.entry(NEXT_ROUTE, Channels.base(() -> ROUTE_FINALIZE)),
            Map.entry(PENDING_TOOL, Channels.base(() -> "")),
            Map.entry(PENDING_ARGUMENTS, Channels.base(() -> new HashMap<String, Object>())),
            Map.entry(LAST_OBSERVATION, Channels.base(() -> "")),
            Map.entry(FINAL_ANSWER, Channels.base(() -> "")),
            Map.entry(COMPLETED, Channels.base(() -> Boolean.FALSE)),
            Map.entry(STATUS, Channels.base(() -> STATUS_RUNNING))
    );

    public WorkforgeAgentState(Map<String, Object> initData) {
        super(initData);
    }

    public String userMessage() {
        return value(USER_MESSAGE, "");
    }

    public List<String> context() {
        return value(CONTEXT, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> toolCalls() {
        return value(TOOL_CALLS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> toolResults() {
        return value(TOOL_RESULTS, List.of());
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> steps() {
        return value(STEPS, List.of());
    }

    public String currentNode() {
        return value(CURRENT_NODE, "");
    }

    public int iteration() {
        Object raw = value(ITERATION).orElse(0);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    public int maxIterations() {
        Object raw = value(MAX_ITERATIONS).orElse(5);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return 5;
    }

    public String nextRoute() {
        return value(NEXT_ROUTE, ROUTE_FINALIZE);
    }

    public String pendingTool() {
        return value(PENDING_TOOL, "");
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> pendingArguments() {
        Object raw = value(PENDING_ARGUMENTS).orElse(Map.of());
        if (raw instanceof Map<?, ?> map) {
            return new HashMap<>((Map<String, Object>) map);
        }
        return new HashMap<>();
    }

    public String lastObservation() {
        return value(LAST_OBSERVATION, "");
    }

    public String finalAnswer() {
        return value(FINAL_ANSWER, "");
    }

    public boolean completed() {
        Object raw = value(COMPLETED).orElse(Boolean.FALSE);
        return Boolean.TRUE.equals(raw);
    }

    public String status() {
        return value(STATUS, STATUS_RUNNING);
    }

    public boolean maxIterationsReached() {
        return iteration() >= maxIterations();
    }
}
