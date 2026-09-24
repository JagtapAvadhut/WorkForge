package com.avadhoot.workforgeai.ai.controller;

import com.avadhoot.workforgeai.ai.dto.ChatRequest;
import com.avadhoot.workforgeai.ai.dto.ChatResponse;
import com.avadhoot.workforgeai.ai.dto.PromptPreviewResponse;
import com.avadhoot.workforgeai.ai.pipeline.AiRequestPipeline;
import com.avadhoot.workforgeai.ai.service.AiChatService;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final AiRequestPipeline aiRequestPipeline;

    public AiChatController(AiChatService aiChatService, AiRequestPipeline aiRequestPipeline) {
        this.aiChatService = aiChatService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "CHAT",
                request.message(),
                () -> aiChatService.chat(request)));
    }

    @PostMapping("/prompt-preview")
    public ApiResponse<PromptPreviewResponse> promptPreview(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.ok(aiChatService.preview(request));
    }
}
