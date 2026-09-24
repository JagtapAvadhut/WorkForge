package com.avadhoot.workforgeai.ai.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Wraps tool callbacks to capture name/arguments/result for learning responses.
 */
public final class RecordingToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final List<RecordedToolCall> sink;
    private final JsonMapper jsonMapper;

    public RecordingToolCallback(ToolCallback delegate, List<RecordedToolCall> sink, JsonMapper jsonMapper) {
        this.delegate = delegate;
        this.sink = sink;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        String result = delegate.call(toolInput);
        sink.add(new RecordedToolCall(
                delegate.getToolDefinition().name(),
                parseArguments(toolInput),
                summarize(result)));
        return result;
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String result = delegate.call(toolInput, toolContext);
        sink.add(new RecordedToolCall(
                delegate.getToolDefinition().name(),
                parseArguments(toolInput),
                summarize(result)));
        return result;
    }

    private Map<String, Object> parseArguments(String toolInput) {
        if (toolInput == null || toolInput.isBlank()) {
            return Map.of();
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = jsonMapper.readValue(toolInput, Map.class);
            return map == null ? Map.of() : map;
        } catch (Exception ex) {
            return Map.of("raw", toolInput);
        }
    }

    private static String summarize(String result) {
        if (result == null) {
            return "";
        }
        String trimmed = result.trim();
        if (trimmed.length() <= 400) {
            return trimmed;
        }
        return trimmed.substring(0, 397) + "...";
    }

    public static List<RecordedToolCall> newSink() {
        return new CopyOnWriteArrayList<>();
    }

    public static List<ToolCallback> wrapAll(ToolCallback[] callbacks, List<RecordedToolCall> sink, JsonMapper jsonMapper) {
        List<ToolCallback> wrapped = new ArrayList<>(callbacks.length);
        for (ToolCallback callback : callbacks) {
            wrapped.add(new RecordingToolCallback(callback, sink, jsonMapper));
        }
        return Collections.unmodifiableList(wrapped);
    }

    public record RecordedToolCall(String tool, Map<String, Object> arguments, String resultSummary) {
    }
}
