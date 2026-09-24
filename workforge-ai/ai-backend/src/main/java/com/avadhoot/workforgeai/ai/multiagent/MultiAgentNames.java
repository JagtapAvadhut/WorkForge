package com.avadhoot.workforgeai.ai.multiagent;

/**
 * Agent and node identifiers for the multi-agent graph.
 */
public final class MultiAgentNames {

    public static final String SUPERVISOR = "SUPERVISOR";
    public static final String ISSUE_AGENT = "ISSUE_AGENT";
    public static final String KNOWLEDGE_AGENT = "KNOWLEDGE_AGENT";
    public static final String PROJECT_AGENT = "PROJECT_AGENT";
    public static final String FINALIZE = "FINALIZE";

    public static final String ROUTE_SUPERVISOR = "supervisor";
    public static final String ROUTE_ISSUE = "issue";
    public static final String ROUTE_KNOWLEDGE = "knowledge";
    public static final String ROUTE_PROJECT = "project";
    public static final String ROUTE_FINALIZE = "finalize";

    private MultiAgentNames() {
    }

    public static String routeForAgent(String agent) {
        if (agent == null) {
            return ROUTE_FINALIZE;
        }
        return switch (agent.trim().toUpperCase()) {
            case ISSUE_AGENT -> ROUTE_ISSUE;
            case KNOWLEDGE_AGENT -> ROUTE_KNOWLEDGE;
            case PROJECT_AGENT -> ROUTE_PROJECT;
            default -> ROUTE_FINALIZE;
        };
    }

    public static String agentForRoute(String route) {
        if (route == null) {
            return "";
        }
        return switch (route.trim().toLowerCase()) {
            case ROUTE_ISSUE -> ISSUE_AGENT;
            case ROUTE_KNOWLEDGE -> KNOWLEDGE_AGENT;
            case ROUTE_PROJECT -> PROJECT_AGENT;
            default -> "";
        };
    }
}
