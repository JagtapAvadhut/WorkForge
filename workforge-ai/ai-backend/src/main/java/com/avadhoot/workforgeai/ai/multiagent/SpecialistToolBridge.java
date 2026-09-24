package com.avadhoot.workforgeai.ai.multiagent;

import com.avadhoot.workforgeai.ai.mcp.McpClientGateway;
import com.avadhoot.workforgeai.ai.mcp.McpToolException;
import com.avadhoot.workforgeai.ai.mcp.McpUnavailableException;
import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.security.SecurityPolicyService;
import com.avadhoot.workforgeai.ai.tools.RecordingToolCallback;
import com.avadhoot.workforgeai.ai.tools.WorkforgeTools;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Prefer MCP tools; fall back to Phase 6 WorkforgeTools only when MCP is unavailable.
 */
@Component
public class SpecialistToolBridge {

    private static final Set<String> ALLOWED = Set.of("getissue", "searchissues", "getproject");

    private final McpClientGateway mcpClientGateway;
    private final Map<String, ToolCallback> localCallbacks;
    private final JsonMapper jsonMapper;
    private final SecurityPolicyService securityPolicyService;

    public SpecialistToolBridge(
            McpClientGateway mcpClientGateway,
            WorkforgeTools workforgeTools,
            JsonMapper jsonMapper,
            SecurityPolicyService securityPolicyService) {
        this.mcpClientGateway = mcpClientGateway;
        this.jsonMapper = jsonMapper;
        this.securityPolicyService = securityPolicyService;
        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(workforgeTools)
                .build()
                .getToolCallbacks();
        this.localCallbacks = new LinkedHashMap<>();
        for (ToolCallback callback : callbacks) {
            localCallbacks.put(callback.getToolDefinition().name().toLowerCase(Locale.ROOT), callback);
        }
    }

    public ToolCallOutcome call(String toolName, Map<String, Object> arguments) {
        if (!StringUtils.hasText(toolName)) {
            return ToolCallOutcome.failure(toolName, Map.of(), "tool name is required", "none");
        }
        String normalized = toolName.trim();
        securityPolicyService.assertToolAllowed(normalized);
        if (!ALLOWED.contains(normalized.toLowerCase(Locale.ROOT))) {
            return ToolCallOutcome.failure(normalized, arguments, "Tool not allowlisted: " + normalized, "none");
        }

        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        if (mcpClientGateway.status().serverUp()) {
            try {
                String result = mcpClientGateway.executeTool(normalized, args);
                // MCP gateway already increments observability counters
                return ToolCallOutcome.success(normalized, args, summarize(result), "mcp");
            } catch (McpUnavailableException | McpToolException ex) {
                // fall through to local
            } catch (Exception ex) {
                return ToolCallOutcome.failure(
                        normalized, args, "MCP tool failure: " + safe(ex), "mcp");
            }
        }

        ToolCallback local = localCallbacks.get(normalized.toLowerCase(Locale.ROOT));
        if (local == null) {
            return ToolCallOutcome.failure(normalized, args, "Local tool unavailable: " + normalized, "local");
        }
        try {
            String argsJson = jsonMapper.writeValueAsString(args);
            List<RecordingToolCallback.RecordedToolCall> sink = RecordingToolCallback.newSink();
            String result = new RecordingToolCallback(local, sink, jsonMapper).call(argsJson);
            AiExecutionContext ctx = AiExecutionContext.current();
            if (ctx != null) {
                ctx.addToolCall();
            }
            return ToolCallOutcome.success(normalized, args, summarize(result), "local");
        } catch (Exception ex) {
            return ToolCallOutcome.failure(normalized, args, "Local tool failure: " + safe(ex), "local");
        }
    }

    private static String summarize(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 800) {
            return trimmed;
        }
        return trimmed.substring(0, 797) + "...";
    }

    private static String safe(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    public record ToolCallOutcome(
            String tool,
            Map<String, Object> arguments,
            String result,
            boolean ok,
            String via
    ) {
        public static ToolCallOutcome success(String tool, Map<String, Object> args, String result, String via) {
            return new ToolCallOutcome(tool, args, result, true, via);
        }

        public static ToolCallOutcome failure(String tool, Map<String, Object> args, String result, String via) {
            return new ToolCallOutcome(tool, args == null ? Map.of() : args, result, false, via);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("tool", tool);
            map.put("arguments", arguments);
            map.put("result", result);
            map.put("ok", ok);
            map.put("via", via);
            return map;
        }
    }
}
