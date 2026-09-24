package com.avadhoot.workforgeai.ai.embedding.controller;

import com.avadhoot.workforgeai.ai.embedding.dto.BatchEmbedRequest;
import com.avadhoot.workforgeai.ai.embedding.dto.BatchEmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.CompareRequest;
import com.avadhoot.workforgeai.ai.embedding.dto.CompareResult;
import com.avadhoot.workforgeai.ai.embedding.dto.EmbedTextRequest;
import com.avadhoot.workforgeai.ai.embedding.dto.EmbeddingResult;
import com.avadhoot.workforgeai.ai.embedding.dto.SimilarityRequest;
import com.avadhoot.workforgeai.ai.embedding.dto.SimilarityResult;
import com.avadhoot.workforgeai.ai.embedding.service.EmbeddingService;
import com.avadhoot.workforgeai.ai.embedding.service.EmbeddingSimilarityService;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;
    private final EmbeddingSimilarityService similarityService;

    public EmbeddingController(
            EmbeddingService embeddingService,
            EmbeddingSimilarityService similarityService) {
        this.embeddingService = embeddingService;
        this.similarityService = similarityService;
    }

    @PostMapping
    public ApiResponse<EmbeddingResult> embed(@Valid @RequestBody EmbedTextRequest request) {
        return ApiResponse.ok(embeddingService.embedAsResult(request.text()));
    }

    @PostMapping("/similarity")
    public ApiResponse<SimilarityResult> similarity(@Valid @RequestBody SimilarityRequest request) {
        return ApiResponse.ok(similarityService.similarity(request.text1(), request.text2()));
    }

    @PostMapping("/batch")
    public ApiResponse<BatchEmbeddingResult> batch(@Valid @RequestBody BatchEmbedRequest request) {
        return ApiResponse.ok(embeddingService.embedBatch(request.texts()));
    }

    @PostMapping("/compare")
    public ApiResponse<CompareResult> compare(@Valid @RequestBody CompareRequest request) {
        return ApiResponse.ok(similarityService.compare(request.query(), request.documents()));
    }
}
