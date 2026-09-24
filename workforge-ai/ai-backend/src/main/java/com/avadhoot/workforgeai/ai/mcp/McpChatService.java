package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.mcp.dto.McpChatRequest;
import com.avadhoot.workforgeai.ai.mcp.dto.McpChatResponse;
import com.avadhoot.workforgeai.ai.tools.RecordingToolCallback;
import com.avadhoot.workforgeai.ai.tools.ToolCallTextParser;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chat that uses only MCP-discovered tools (never local Phase 6 ToolCallbacks).
 */
@Service
public class McpChatService {

    private static final int MAX_FALLBACK_ROUNDS = 3;

    private final ChatClient chatClient;
    private final McpClientGateway mcpClientGateway;
    private final JsonMapper jsonMapper;

    public McpChatService(ChatClient chatClient, McpClientGateway mcpClientGateway, JsonMapper jsonMapper) {
        this.chatClient = chatClient;
        this.mcpClientGateway = mcpClientGateway;
        this.jsonMapper = jsonMapper;
    }

    public McpChatResponse chat(McpChatRequest request) {
        if (request == null || !StringUtils.hasText(request.message())) {
            throw new IllegalArgumentException("message must not be blank");
        }
        List<ToolCallback> mcpCallbacks = mcpClientGateway.allowedToolCallbacks();
        if (mcpCallbacks.isEmpty() && !mcpClientGateway.status().serverUp()) {
            throw new McpUnavailableException("MCP server is unavailable; cannot run mcp-chat");
        }

        String userMessage = request.message().trim();
        List<RecordingToolCallback.RecordedToolCall> sink = RecordingToolCallback.newSink();
        List<ToolCallback> wrapped = RecordingToolCallback.wrapAll(
                mcpCallbacks.toArray(ToolCallback[]::new), sink, jsonMapper);
        Map<String, ToolCallback> byName = indexByCanonicalName(mcpCallbacks);

        String content = chatClient.prompt()
                .system(McpPrompt.CHAT_SYSTEM)
                .user(userMessage)
                .toolCallbacks(wrapped)
                .call()
                .content();

        List<String> fallbackResults = new ArrayList<>();
        int rounds = 0;
        while (sink.isEmpty() && rounds < MAX_FALLBACK_ROUNDS && StringUtils.hasText(content)) {
            var parsed = ToolCallTextParser.parse(content, jsonMapper)
                    .filter(call -> byName.containsKey(call.name().toLowerCase(Locale.ROOT))
                            || byName.containsKey(call.name()));
            if (parsed.isEmpty()) {
                break;
            }
            rounds++;
            ToolCallTextParser.ParsedToolCall call = parsed.get();
            ToolCallback callback = byName.getOrDefault(
                    call.name().toLowerCase(Locale.ROOT),
                    byName.get(call.name()));
            if (callback == null) {
                break;
            }
            String toolResult = new RecordingToolCallback(callback, sink, jsonMapper).call(call.argumentsJson());
            fallbackResults.add(call.name() + " => " + toolResult);
            content = chatClient.prompt()
                    .system(McpPrompt.CHAT_SYSTEM + """

                            You already received MCP tool results below.
                            Answer using those results. Do not print tool-call JSON.
                            """)
                    .user("""
                            User question:
                            %s

                            MCP tool results:
                            %s
                            """.formatted(userMessage, String.join("\n", fallbackResults)))
                    .call()
                    .content();
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalStateException("The model returned an empty MCP chat response");
        }

        List<Map<String, Object>> toolsUsed = sink.stream()
                .map(call -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("name", call.tool());
                    row.put("arguments", call.arguments());
                    row.put("result", call.resultSummary());
                    return row;
                })
                .toList();

        return new McpChatResponse(content.trim(), toolsUsed);
    }

    private static Map<String, ToolCallback> indexByCanonicalName(List<ToolCallback> callbacks) {
        Map<String, ToolCallback> map = new LinkedHashMap<>();
        for (ToolCallback callback : callbacks) {
            String name = callback.getToolDefinition().name();
            map.put(name, callback);
            map.put(name.toLowerCase(Locale.ROOT), callback);
            int idx = name.lastIndexOf('_');
            if (idx > 0) {
                String shortName = name.substring(idx + 1);
                map.put(shortName, callback);
                map.put(shortName.toLowerCase(Locale.ROOT), callback);
            }
        }
        return map;
    }
}
