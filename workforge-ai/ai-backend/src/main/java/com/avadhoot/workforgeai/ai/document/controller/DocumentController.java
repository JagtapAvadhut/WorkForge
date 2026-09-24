package com.avadhoot.workforgeai.ai.document.controller;

import com.avadhoot.workforgeai.ai.document.dto.CreateDocumentRequest;
import com.avadhoot.workforgeai.ai.document.dto.DocumentPageResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchRequest;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchResponse;
import com.avadhoot.workforgeai.ai.document.service.DocumentIngestionService;
import com.avadhoot.workforgeai.ai.document.service.DocumentQueryService;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/documents")
public class DocumentController {

    private final DocumentIngestionService ingestionService;
    private final DocumentQueryService queryService;

    public DocumentController(
            DocumentIngestionService ingestionService,
            DocumentQueryService queryService) {
        this.ingestionService = ingestionService;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        return ApiResponse.ok(ingestionService.ingest(request));
    }

    @PostMapping("/search")
    public ApiResponse<DocumentSearchResponse> search(@Valid @RequestBody DocumentSearchRequest request) {
        return ApiResponse.ok(queryService.search(request));
    }

    @GetMapping
    public ApiResponse<DocumentPageResponse> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(queryService.list(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(queryService.get(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        ingestionService.delete(id);
        return ApiResponse.ok(null);
    }
}
