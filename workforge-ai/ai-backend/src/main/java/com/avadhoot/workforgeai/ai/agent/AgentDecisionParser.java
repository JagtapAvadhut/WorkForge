package com.avadhoot.workforgeai.ai.agent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses one agent decision JSON object from the model.
 */
public final class AgentDecisionParser {

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");

    private AgentDecisionParser() {
    }

    public static Optional<AgentDecision> parse(String content, JsonMapper jsonMapper) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        String trimmed = content.trim();
        Optional<AgentDecision> direct = tryParse(trimmed, jsonMapper);
        if (direct.isPresent()) {
            return direct;
        }
        Matcher matcher = JSON_OBJECT.matcher(trimmed);
        while (matcher.find()) {
            Optional<AgentDecision> parsed = tryParse(matcher.group(), jsonMapper);
            if (parsed.isPresent()) {
                return parsed;
            }
        }
        return Optional.empty();
    }

    private static Optional<AgentDecision> tryParse(String candidate, JsonMapper jsonMapper) {
        try {
            JsonNode node = jsonMapper.readTree(candidate);
            if (node == null || !node.isObject()) {
                return Optional.empty();
            }
            String action = text(node, "action");
            if (action == null) {
                return Optional.empty();
            }
            action = action.trim().toLowerCase();
            String thought = text(node, "thought");
            if ("final".equals(action) || "finish".equals(action) || "answer".equals(action)) {
                String answer = text(node, "answer");
                if (answer == null || answer.isBlank()) {
                    answer = text(node, "response");
                }
                if (answer == null || answer.isBlank()) {
                    return Optional.empty();
                }
                return Optional.of(AgentDecision.finalAnswer(thought, answer.trim()));
            }
            if ("tool".equals(action) || "call_tool".equals(action) || "call".equals(action)) {
                String tool = text(node, "tool");
                if (tool == null || tool.isBlank()) {
                    tool = text(node, "name");
                }
                if (tool == null || tool.isBlank()) {
                    return Optional.empty();
                }
                Map<String, Object> args = readArgs(node.get("arguments"), jsonMapper);
                if (args.isEmpty()) {
                    args = readArgs(node.get("parameters"), jsonMapper);
                }
                return Optional.of(AgentDecision.tool(thought, tool.trim(), args));
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readArgs(JsonNode argsNode, JsonMapper jsonMapper) {
        if (argsNode == null || argsNode.isNull()) {
            return Map.of();
        }
        try {
            if (argsNode.isObject()) {
                Map<String, Object> map = jsonMapper.convertValue(argsNode, Map.class);
                return map == null ? Map.of() : new LinkedHashMap<>(map);
            }
            if (argsNode.isString()) {
                Map<String, Object> map = jsonMapper.readValue(argsNode.asString(), Map.class);
                return map == null ? Map.of() : new LinkedHashMap<>(map);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return Map.of();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    public record AgentDecision(
            String action,
            String thought,
            String tool,
            Map<String, Object> arguments,
            String answer
    ) {
        public static AgentDecision tool(String thought, String tool, Map<String, Object> arguments) {
            return new AgentDecision("tool", thought, tool, arguments == null ? Map.of() : arguments, null);
        }

        public static AgentDecision finalAnswer(String thought, String answer) {
            return new AgentDecision("final", thought, null, Map.of(), answer);
        }

        public boolean isTool() {
            return "tool".equals(action);
        }

        public boolean isFinal() {
            return "final".equals(action);
        }
    }
}
