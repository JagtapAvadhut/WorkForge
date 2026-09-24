package com.avadhoot.workforgeai.ai.mcp;

import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentRequest;
import com.avadhoot.workforgeai.ai.mcp.dto.McpAgentResponse;
import com.avadhoot.workforgeai.ai.mcp.dto.McpChatRequest;
import com.avadhoot.workforgeai.ai.mcp.dto.McpChatResponse;
import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.pipeline.AiRequestPipeline;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class McpController {

    private final McpClientGateway mcpClientGateway;
    private final McpChatService mcpChatService;
    private final McpAgentService mcpAgentService;
    private final AiRequestPipeline aiRequestPipeline;

    public McpController(
            McpClientGateway mcpClientGateway,
            McpChatService mcpChatService,
            McpAgentService mcpAgentService,
            AiRequestPipeline aiRequestPipeline) {
        this.mcpClientGateway = mcpClientGateway;
        this.mcpChatService = mcpChatService;
        this.mcpAgentService = mcpAgentService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @GetMapping("/mcp/status")
    public ApiResponse<McpClientGateway.McpConnectionStatus> status() {
        return ApiResponse.ok(mcpClientGateway.status());
    }

    @GetMapping("/mcp/tools")
    public ApiResponse<List<McpClientGateway.McpToolDescriptor>> tools() {
        return ApiResponse.ok(mcpClientGateway.discoverTools());
    }

    @PostMapping("/mcp-chat")
    public ApiResponse<McpChatResponse> chat(@Valid @RequestBody McpChatRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "MCP_CHAT",
                request.message(),
                () -> mcpChatService.chat(request),
                response -> {
                    AiExecutionContext ctx = AiExecutionContext.current();
                    if (ctx != null && response.toolsUsed() != null) {
                        ctx.addMcpCalls(response.toolsUsed().size());
                        ctx.addToolCalls(response.toolsUsed().size());
                    }
                }));
    }

    @PostMapping("/mcp-agent")
    public ApiResponse<McpAgentResponse> agent(@Valid @RequestBody McpAgentRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "MCP_AGENT",
                request.message(),
                () -> mcpAgentService.run(request),
                response -> {
                    AiExecutionContext ctx = AiExecutionContext.current();
                    if (ctx == null) {
                        return;
                    }
                    if (response.steps() != null) {
                        ctx.addAgentSteps(response.steps().size());
                    }
                    if (response.toolsUsed() != null) {
                        ctx.addMcpCalls(response.toolsUsed().size());
                        ctx.addToolCalls(response.toolsUsed().size());
                    }
                }));
    }
}
