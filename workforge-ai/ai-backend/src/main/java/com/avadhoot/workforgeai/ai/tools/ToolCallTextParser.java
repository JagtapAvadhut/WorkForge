package com.avadhoot.workforgeai.ai.tools;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallback parser for models that print tool calls as JSON text instead of native tool_calls.
 */
public final class ToolCallTextParser {

    private static final Pattern JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*}");

    private ToolCallTextParser() {
    }

    public static Optional<ParsedToolCall> parse(String content, JsonMapper jsonMapper) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        String trimmed = content.trim();
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);
        Matcher matcher = JSON_OBJECT.matcher(trimmed);
        while (matcher.find()) {
            candidates.add(matcher.group());
        }

        for (String candidate : candidates) {
            try {
                JsonNode node = jsonMapper.readTree(candidate);
                if (node == null || !node.isObject()) {
                    continue;
                }
                String name = text(node, "name");
                if (name == null || name.isBlank()) {
                    name = text(node, "tool");
                }
                JsonNode args = node.get("arguments");
                if (args == null) {
                    args = node.get("parameters");
                }
                if (name == null || name.isBlank() || args == null || !args.isObject()) {
                    continue;
                }
                return Optional.of(new ParsedToolCall(name.trim(), jsonMapper.writeValueAsString(args)));
            } catch (Exception ignored) {
                // try next candidate
            }
        }
        return Optional.empty();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    public record ParsedToolCall(String name, String argumentsJson) {
    }
}
