package com.avadhoot.workforgeai.ai.multiagent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses supervisor JSON decisions (delegate / finalize). Does not store thoughts.
 */
public final class SupervisorDecisionParser {

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");
    private static final Set<String> KNOWN_AGENTS = Set.of(
            MultiAgentNames.ISSUE_AGENT,
            MultiAgentNames.KNOWLEDGE_AGENT,
            MultiAgentNames.PROJECT_AGENT);

    private SupervisorDecisionParser() {
    }

    public static Optional<SupervisorDecision> parse(String content, JsonMapper jsonMapper) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        String trimmed = content.trim();
        Optional<SupervisorDecision> direct = tryParse(trimmed, jsonMapper);
        if (direct.isPresent()) {
            return direct;
        }
        Matcher matcher = JSON_OBJECT.matcher(trimmed);
        while (matcher.find()) {
            Optional<SupervisorDecision> parsed = tryParse(matcher.group(), jsonMapper);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<SupervisorDecision> tryParse(String candidate, JsonMapper jsonMapper) {
        try {
            JsonNode node = jsonMapper.readTree(candidate);
            if (node == null || !node.isObject()) {
                return Optional.empty();
            }
            String action = text(node, "action");
            if (action == null) {
                return Optional.empty();
            }
            action = action.trim().toLowerCase(Locale.ROOT);
            if ("final".equals(action) || "finalize".equals(action) || "answer".equals(action) || "finish".equals(action)) {
                String answer = text(node, "answer");
                if (answer == null || answer.isBlank()) {
                    answer = text(node, "response");
                }
                if (answer == null || answer.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(SupervisorDecision.finalizeAnswer(answer.trim()));
            }
            if ("delegate".equals(action) || "route".equals(action) || "assign".equals(action)) {
                List<String> agents = readAgents(node);
                if (agents.isEmpty()) {
                    return Optional.empty();
                }
                String task = text(node, "task");
                if (task == null || task.isBlank()) {
                    task = text(node, "instruction");
                }
                if (task == null) {
                    task = "";
                }
                return Optional.of(SupervisorDecision.delegate(agents, task.trim()));
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private static List<String> readAgents(JsonNode node) {
        LinkedHashSet<String> agents = new LinkedHashSet<>();
        JsonNode array = node.get("agents");
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                addAgent(agents, item == null ? null : item.asString());
            }
        }
        addAgent(agents, text(node, "agent"));
        return List.copyOf(agents);
    }

    private static void addAgent(Set<String> agents, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if ("ISSUE".equals(normalized) || "ISSUES".equals(normalized)) {
            normalized = MultiAgentNames.ISSUE_AGENT;
        } else if ("KNOWLEDGE".equals(normalized) || "RAG".equals(normalized) || "DOCS".equals(normalized)) {
            normalized = MultiAgentNames.KNOWLEDGE_AGENT;
        } else if ("PROJECT".equals(normalized) || "PROJECTS".equals(normalized)) {
            normalized = MultiAgentNames.PROJECT_AGENT;
        }
        if (KNOWN_AGENTS.contains(normalized)) {
            agents.add(normalized);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        String value = child.asString();
        return value == null || value.isBlank() ? null : value;
    }

    public record SupervisorDecision(String action, List<String> agents, String task, String answer) {
        public static SupervisorDecision delegate(List<String> agents, String task) {
            return new SupervisorDecision("delegate", List.copyOf(agents), task == null ? "" : task, null);
        }

        public static SupervisorDecision finalizeAnswer(String answer) {
            return new SupervisorDecision("finalize", List.of(), "", answer);
        }

        public boolean isDelegate() {
            return "delegate".equals(action);
        }

        public boolean isFinalize() {
            return "finalize".equals(action);
        }
    }

    /**
     * Deterministic fallback when the model returns unusable JSON.
     */
    public static SupervisorDecision heuristic(String userRequest, List<Map<String, Object>> existingResults) {
        String text = userRequest == null ? "" : userRequest.toLowerCase(Locale.ROOT);
        boolean hasResults = existingResults != null && !existingResults.isEmpty();
        if (hasResults) {
            return SupervisorDecision.finalizeAnswer(combineFallback(existingResults));
        }
        List<String> agents = new ArrayList<>();
        boolean wantsIssue = text.contains("issue") || text.contains("mws-") || text.contains("investigate")
                || text.matches("(?s).*\\b[a-z]{2,10}-\\d+\\b.*");
        boolean wantsProject = text.contains("project")
                || (text.contains("analysis") && (text.contains("mws") || text.contains("project")))
                || (text.contains("mws") && !text.contains("mws-") && (text.contains("project") || text.contains("analysis")));
        boolean wantsKnowledge = text.contains("document") || text.contains("documentation")
                || text.contains("relevant doc") || text.contains("knowledge base")
                || text.contains("pgvector") || text.contains(" from the docs");

        if (text.contains("completely") || (text.contains("investigate") && text.contains("mws-"))) {
            agents.add(MultiAgentNames.ISSUE_AGENT);
            agents.add(MultiAgentNames.PROJECT_AGENT);
            agents.add(MultiAgentNames.KNOWLEDGE_AGENT);
        } else {
            if (wantsIssue) {
                agents.add(MultiAgentNames.ISSUE_AGENT);
            }
            if (wantsProject) {
                agents.add(MultiAgentNames.PROJECT_AGENT);
            }
            if (wantsKnowledge) {
                agents.add(MultiAgentNames.KNOWLEDGE_AGENT);
            }
            // "explain X and relevant documentation" style
            if (text.contains("explain") && text.contains("mws-") && !agents.contains(MultiAgentNames.KNOWLEDGE_AGENT)) {
                agents.add(MultiAgentNames.KNOWLEDGE_AGENT);
            }
        }

        if (agents.isEmpty()) {
            // conceptual — finalize without specialists
            return SupervisorDecision.finalizeAnswer(
                    "A sprint is a fixed-length iteration used to plan and deliver work in WorkForge.");
        }
        return SupervisorDecision.delegate(agents, userRequest == null ? "" : userRequest.trim());
    }

    private static String combineFallback(List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder("Combined specialist findings:\n");
        for (Map<String, Object> result : results) {
            sb.append("- ")
                    .append(result.getOrDefault("agent", "?"))
                    .append(": ")
                    .append(result.getOrDefault("summary", ""))
                    .append('\n');
        }
        return sb.toString().trim();
    }
}
