package com.avadhoot.workforgeai.ai.mcp;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gateway to MCP-discovered tools. Never falls back to local Phase 6 Java tools.
 */
@Service
public class McpClientGateway {

    private final ObjectProvider<ToolCallbackProvider> mcpToolCallbackProvider;
    private final JsonMapper jsonMapper;
    private final String serverUrl;
    private final String mcpEndpoint;
    private final Set<String> allowedTools;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public McpClientGateway(
            ObjectProvider<ToolCallbackProvider> mcpToolCallbackProvider,
            JsonMapper jsonMapper,
            @Value("${workforge.ai.mcp.server-url:http://localhost:8091}") String serverUrl,
            @Value("${workforge.ai.mcp.endpoint:/mcp}") String mcpEndpoint,
            @Value("${workforge.ai.mcp.allowed-tools:getIssue,searchIssues,getProject}") String allowedToolsCsv) {
        this.mcpToolCallbackProvider = mcpToolCallbackProvider;
        this.jsonMapper = jsonMapper;
        this.serverUrl = trimTrailingSlash(serverUrl);
        this.mcpEndpoint = mcpEndpoint.startsWith("/") ? mcpEndpoint : "/" + mcpEndpoint;
        this.allowedTools = Arrays.stream(allowedToolsCsv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public McpConnectionStatus status() {
        boolean serverUp = pingServer();
        boolean clientReady = false;
        String detail = serverUp ? "MCP server reachable" : "MCP server unreachable at " + serverUrl;
        if (serverUp) {
            try {
                clientReady = !discoverCallbacks().isEmpty() || true;
                // Probe discovery; empty tools still means client connected if no exception
                discoverTools();
                clientReady = true;
                detail = "MCP client connected to " + serverUrl + mcpEndpoint;
            } catch (McpUnavailableException ex) {
                clientReady = false;
                detail = ex.getMessage();
            }
        }
        return new McpConnectionStatus(serverUp, clientReady, serverUrl + mcpEndpoint, detail);
    }

    public List<McpToolDescriptor> discoverTools() {
        List<ToolCallback> callbacks = discoverCallbacks();
        List<McpToolDescriptor> tools = new ArrayList<>();
        for (ToolCallback callback : callbacks) {
            ToolDefinition def = callback.getToolDefinition();
            String name = stripPrefix(def.name());
            if (!isAllowed(name)) {
                continue;
            }
            tools.add(new McpToolDescriptor(
                    name,
                    def.description(),
                    parseSchema(def.inputSchema())));
        }
        return List.copyOf(tools);
    }

    public List<ToolCallback> allowedToolCallbacks() {
        return discoverCallbacks().stream()
                .filter(cb -> isAllowed(stripPrefix(cb.getToolDefinition().name())))
                .toList();
    }

    public String executeTool(String toolName, Map<String, Object> arguments) {
        if (!StringUtils.hasText(toolName)) {
            throw new McpToolException("tool name is required");
        }
        String normalized = toolName.trim();
        if (!isAllowed(normalized)) {
            throw new McpToolException("Tool not allowlisted for MCP: " + normalized);
        }
        ToolCallback callback = findCallback(normalized)
                .orElseThrow(() -> new McpToolException("Unknown MCP tool: " + normalized));
        try {
            String argsJson = jsonMapper.writeValueAsString(arguments == null ? Map.of() : arguments);
            String result = callback.call(argsJson);
            var ctx = com.avadhoot.workforgeai.ai.observability.AiExecutionContext.current();
            if (ctx != null) {
                ctx.addMcpCall();
                ctx.addToolCall();
            }
            return result;
        } catch (McpToolException | McpUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new McpToolException("MCP tool execution failed: " + safeMessage(ex), ex);
        }
    }

    private Optional<ToolCallback> findCallback(String toolName) {
        String want = toolName.toLowerCase(Locale.ROOT);
        for (ToolCallback callback : discoverCallbacks()) {
            String name = stripPrefix(callback.getToolDefinition().name()).toLowerCase(Locale.ROOT);
            if (want.equals(name)) {
                return Optional.of(callback);
            }
        }
        return Optional.empty();
    }

    private List<ToolCallback> discoverCallbacks() {
        ToolCallbackProvider provider = mcpToolCallbackProvider.getIfAvailable();
        if (provider == null) {
            throw new McpUnavailableException("MCP client is not configured or disabled");
        }
        try {
            ToolCallback[] callbacks = provider.getToolCallbacks();
            if (callbacks == null || callbacks.length == 0) {
                return List.of();
            }
            return Arrays.asList(callbacks);
        } catch (McpUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new McpUnavailableException("MCP tool discovery failed: " + safeMessage(ex), ex);
        }
    }

    private boolean pingServer() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl + "/actuator/health"))
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isAllowed(String toolName) {
        return allowedTools.contains(toolName.toLowerCase(Locale.ROOT));
    }

    private static String stripPrefix(String name) {
        if (name == null) {
            return "";
        }
        int idx = name.lastIndexOf('_');
        // SyncMcpToolCallbackProvider may prefix with connection name_
        if (idx > 0 && name.substring(0, idx).toLowerCase(Locale.ROOT).contains("mcp")) {
            return name.substring(idx + 1);
        }
        // Also handle "workforge-mcp_getIssue" style
        if (name.contains("_")) {
            String[] parts = name.split("_");
            String last = parts[parts.length - 1];
            if (last.equalsIgnoreCase("getIssue")
                    || last.equalsIgnoreCase("searchIssues")
                    || last.equalsIgnoreCase("getProject")) {
                return last;
            }
        }
        return name;
    }

    private Map<String, Object> parseSchema(String inputSchema) {
        if (!StringUtils.hasText(inputSchema)) {
            return Map.of();
        }
        try {
            JsonNode node = jsonMapper.readTree(inputSchema);
            if (node == null || !node.isObject()) {
                return Map.of("raw", inputSchema);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = jsonMapper.convertValue(node, Map.class);
            return map == null ? Map.of() : new LinkedHashMap<>(map);
        } catch (Exception ex) {
            return Map.of("raw", inputSchema);
        }
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String safeMessage(Exception ex) {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    public record McpConnectionStatus(
            boolean serverUp,
            boolean clientConnected,
            String endpoint,
            String detail
    ) {
    }

    public record McpToolDescriptor(
            String name,
            String description,
            Map<String, Object> parameters
    ) {
    }
}
