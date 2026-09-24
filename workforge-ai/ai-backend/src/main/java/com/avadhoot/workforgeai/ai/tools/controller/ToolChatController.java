package com.avadhoot.workforgeai.ai.tools.controller;

import com.avadhoot.workforgeai.ai.observability.AiExecutionContext;
import com.avadhoot.workforgeai.ai.pipeline.AiRequestPipeline;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatRequest;
import com.avadhoot.workforgeai.ai.tools.dto.ToolChatResponse;
import com.avadhoot.workforgeai.ai.tools.dto.ToolPreviewResponse;
import com.avadhoot.workforgeai.ai.tools.service.ToolChatService;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class ToolChatController {

    private final ToolChatService toolChatService;
    private final AiRequestPipeline aiRequestPipeline;

    public ToolChatController(ToolChatService toolChatService, AiRequestPipeline aiRequestPipeline) {
        this.toolChatService = toolChatService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @PostMapping("/tool-chat")
    public ApiResponse<ToolChatResponse> toolChat(@Valid @RequestBody ToolChatRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "TOOL",
                request.message(),
                () -> toolChatService.chat(request),
                response -> {
                    AiExecutionContext ctx = AiExecutionContext.current();
                    if (ctx != null && response.toolCalls() != null) {
                        ctx.addToolCalls(response.toolCalls().size());
                    }
                }));
    }

    @PostMapping("/tool-preview")
    public ApiResponse<ToolPreviewResponse> toolPreview(@Valid @RequestBody ToolChatRequest request) {
        return ApiResponse.ok(toolChatService.preview(request));
    }
}
