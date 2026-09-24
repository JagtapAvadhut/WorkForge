package com.avadhoot.workforgeai.ai.rag;

import com.avadhoot.workforgeai.ai.pipeline.AiRequestPipeline;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryRequest;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/rag")
public class RagController {

    private final RagService ragService;
    private final AiRequestPipeline aiRequestPipeline;

    public RagController(RagService ragService, AiRequestPipeline aiRequestPipeline) {
        this.ragService = ragService;
        this.aiRequestPipeline = aiRequestPipeline;
    }

    @PostMapping("/query")
    public ApiResponse<RagQueryResponse> query(@Valid @RequestBody RagQueryRequest request) {
        return ApiResponse.ok(aiRequestPipeline.execute(
                "RAG",
                request.question(),
                () -> ragService.query(request)));
    }
}
