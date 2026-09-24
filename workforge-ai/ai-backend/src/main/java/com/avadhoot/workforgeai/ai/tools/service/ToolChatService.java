package com.avadhoot.workforgeai.ai.tools.service;

import com.avadhoot.workforgeai.ai.tools.RecordingToolCallback;
import com.avadhoot.workforgeai.ai.tools.ToolCallTextParser;
import com.avadhoot.workforgeai.ai.tools.ToolChatPrompt;
import com.avadhoot.workforgeai.ai.tools.WorkforgeTools;
import com.avadhoot.workforgeai.ai.tools.dto.ToolCallInfo;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatRequest;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatResponse;
import com.avadhoot.workforgeai.ai.tools.dto.ToolPreviewResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolChatService {

    private static final int MAX_FALLBACK_TOOL_ROUNDS = 3;

    private final ChatClient chatClient;
    private final JsonMapper jsonMapper;
    private final ToolCallback[] toolCallbacks;
    private final Map<String, ToolCallback> callbacksByName;

    public ToolChatService(ChatClient chatClient, WorkforgeTools workforgeTools, JsonMapper jsonMapper) {
        this.chatClient = chatClient;
        this.jsonMapper = jsonMapper;
        this.toolCallbacks = MethodToolCallbackProvider.builder()
                .toolObjects(workforgeTools)
                .build()
                .getToolCallbacks();
        this.callbacksByName = new LinkedHashMap<>();
        for (ToolCallback callback : toolCallbacks) {
            callbacksByName.put(callback.getToolDefinition().name(), callback);
        }
    }

    public ToolChatResponse chat(ToolChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }

        String userMessage = request.message().trim();
        List<RecordingToolCallback.RecordedToolCall> sink = RecordingToolCallback.newSink();
        List<ToolCallback> callbacks = RecordingToolCallback.wrapAll(toolCallbacks, sink, jsonMapper);

        String content = chatClient.prompt()
                .system(ToolChatPrompt.SYSTEM)
                .user(userMessage)
                .toolCallbacks(callbacks)
                .call()
                .content();

        // Local models sometimes print tool JSON instead of using native tool_calls.
        // Execute those tools in Java and ask the model for a final grounded answer.
        List<String> fallbackResults = new ArrayList<>();
        int rounds = 0;
        while (sink.isEmpty()
                && rounds < MAX_FALLBACK_TOOL_ROUNDS
                && StringUtils.hasText(content)) {
            var parsed = ToolCallTextParser.parse(content, jsonMapper)
                    .filter(call -> callbacksByName.containsKey(call.name()));
            if (parsed.isEmpty()) {
                break;
            }
            rounds++;
            ToolCallTextParser.ParsedToolCall call = parsed.get();
            ToolCallback callback = callbacksByName.get(call.name());
            if (callback == null) {
                break;
            }
            ToolCallback recording = new RecordingToolCallback(callback, sink, jsonMapper);
            String toolResult = recording.call(call.argumentsJson());
            fallbackResults.add(call.name() + " => " + toolResult);

            content = chatClient.prompt()
                    .system(ToolChatPrompt.SYSTEM + """

                            You already received tool results below.
                            Answer the user using those results.
                            Do not print tool-call JSON.
                            Do not invent data missing from the tool results.
                            """)
                    .user("""
                            User question:
                            %s

                            Tool results:
                            %s
                            """.formatted(userMessage, String.join("\n", fallbackResults)))
                    .call()
                    .content();
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("The model returned an empty tool-chat response");
        }

        // If no tools ran and the model still printed JSON-shaped noise, ask once for a plain answer.
        if (sink.isEmpty() && looksLikeSpuriousToolJson(content)) {
            content = chatClient.prompt()
                    .system("""
                            You are WorkForge AI.
                            Answer the conceptual question in plain language.
                            Do not call tools and do not output JSON.
                            """)
                    .user(userMessage)
                    .call()
                    .content();
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("The model returned an empty tool-chat response");
        }

        List<ToolCallInfo> toolCalls = sink.stream()
                .map(r -> new ToolCallInfo(r.tool(), r.arguments(), r.resultSummary()))
                .toList();

        return new ToolChatResponse(content.trim(), toolCalls);
    }

    private boolean looksLikeSpuriousToolJson(String content) {
        return ToolCallTextParser.parse(content, jsonMapper).isPresent();
    }

    public ToolPreviewResponse preview(ToolChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }

        List<ToolPreviewResponse.ToolDefinitionView> tools = Arrays.stream(toolCallbacks)
                .map(callback -> {
                    var def = callback.getToolDefinition();
                    return new ToolPreviewResponse.ToolDefinitionView(
                            def.name(),
                            def.description(),
                            def.inputSchema());
                })
                .toList();

        return new ToolPreviewResponse(request.message().trim(), tools);
    }
}
