package com.avadhoot.workforgeai.ai.multiagent;

public final class MultiAgentPrompt {

    private MultiAgentPrompt() {
    }

    public static String supervisorSystem() {
        return """
                You are the Supervisor Agent for WorkForge AI multi-agent orchestration.
                You coordinate specialists. You do NOT call repositories or tools yourself.

                Specialists:
                - ISSUE_AGENT: issue details/search via MCP tools (getIssue, searchIssues)
                - PROJECT_AGENT: project summary via MCP tools (getProject, searchIssues)
                - KNOWLEDGE_AGENT: documentation via RAG/PGVector

                Decide using JSON only (no markdown):
                1) Delegate one or more specialists:
                {"action":"delegate","agents":["ISSUE_AGENT"],"task":"short task for specialists"}
                2) Or finalize when you can answer (with or without specialist results):
                {"action":"finalize","answer":"clear final answer for the user"}

                Rules:
                - Conceptual questions (e.g. what is a sprint) → finalize directly.
                - Issue keys like MWS-1 → include ISSUE_AGENT.
                - Documentation/explain/relevant docs → include KNOWLEDGE_AGENT.
                - Project analysis / project MWS → include PROJECT_AGENT.
                - Do not repeat specialists that already returned SUCCESS for the same need.
                - Prefer multiple agents in one delegate when the user asks for a complete investigation.
                """;
    }

    public static String supervisorUser(MultiAgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("User request:\n").append(state.userRequest()).append("\n\n");
        sb.append("Supervisor iteration: ").append(state.iteration())
                .append(" / ").append(state.maxIterations()).append('\n');
        sb.append("Specialist calls used: ").append(state.specialistCalls())
                .append(" / ").append(state.maxSpecialistCalls()).append("\n\n");
        if (state.specialistResults().isEmpty()) {
            sb.append("Specialist results: none yet\n");
        } else {
            sb.append("Specialist results:\n");
            for (var result : state.specialistResults()) {
                sb.append("- agent=").append(result.get("agent"))
                        .append(" status=").append(result.get("status"))
                        .append(" summary=").append(result.get("summary")).append('\n');
                Object findings = result.get("findings");
                if (findings != null) {
                    sb.append("  findings=").append(findings).append('\n');
                }
            }
        }
        sb.append("\nReturn one JSON decision.");
        return sb.toString();
    }

    public static String finalizeSystem() {
        return """
                You synthesize a final user-facing answer from structured specialist results.
                Be clear and concise. Cite issue keys and documentation themes when present.
                Do not invent tools or write chain-of-thought.
                """;
    }

    public static String finalizeUser(MultiAgentState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("Original request:\n").append(state.userRequest()).append("\n\n");
        if (state.specialistResults().isEmpty()) {
            sb.append("No specialist results. Answer from general WorkForge knowledge if appropriate.\n");
        } else {
            sb.append("Structured specialist results:\n");
            for (var result : state.specialistResults()) {
                sb.append(result).append('\n');
            }
        }
        if (!state.sources().isEmpty()) {
            sb.append("\nSources:\n").append(state.sources()).append('\n');
        }
        sb.append("\nWrite the final answer only.");
        return sb.toString();
    }
}
