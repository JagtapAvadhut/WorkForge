package com.avadhoot.workforgeai.ai.document.service;

import com.avadhoot.workforgeai.ai.document.dto.DocumentPageResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchHit;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchRequest;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchResponse;
import com.avadhoot.workforgeai.ai.document.repository.AiDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DocumentQueryService {

    private final VectorStore vectorStore;
    private final AiDocumentRepository repository;
    private final DocumentIngestionService ingestionService;
    private final int maxTopK;
    private final int defaultTopK;
    private final int defaultPageSize;
    private final int maxPageSize;

    public DocumentQueryService(
            VectorStore vectorStore,
            AiDocumentRepository repository,
            DocumentIngestionService ingestionService,
            @Value("${workforge.ai.documents.max-top-k:20}") int maxTopK,
            @Value("${workforge.ai.documents.default-top-k:5}") int defaultTopK,
            @Value("${workforge.ai.documents.default-page-size:20}") int defaultPageSize,
            @Value("${workforge.ai.documents.max-page-size:100}") int maxPageSize) {
        this.vectorStore = vectorStore;
        this.repository = repository;
        this.ingestionService = ingestionService;
        this.maxTopK = maxTopK;
        this.defaultTopK = defaultTopK;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
    }

    public DocumentSearchResponse search(DocumentSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.query())) {
            throw new IllegalArgumentException("query must not be blank");
        }

        int topK = request.topK() == null ? defaultTopK : request.topK();
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be greater than 0");
        }
        if (topK > maxTopK) {
            throw new IllegalArgumentException("topK must be at most " + maxTopK);
        }

        List<Document> hits = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(request.query().trim())
                        .topK(topK)
                        .similarityThresholdAll()
                        .build());

        List<DocumentSearchHit> ranked = hits.stream()
                .map(this::toHit)
                .sorted(Comparator.comparingDouble(DocumentSearchHit::similarity).reversed())
                .toList();

        return new DocumentSearchResponse(request.query().trim(), topK, ranked);
    }

    public DocumentPageResponse list(Integer page, Integer size) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? defaultPageSize : size;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
        if (pageSize > maxPageSize) {
            throw new IllegalArgumentException("size must be at most " + maxPageSize);
        }

        long total = repository.count();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        List<DocumentResponse> items = repository.findPage(pageSize, pageNumber * pageSize).stream()
                .map(ingestionService::toResponse)
                .toList();

        return new DocumentPageResponse(items, pageNumber, pageSize, total, totalPages);
    }

    public DocumentResponse get(UUID id) {
        return repository.findById(id)
                .map(ingestionService::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
    }

    private DocumentSearchHit toHit(Document document) {
        UUID id = UUID.fromString(document.getId());
        double similarity = document.getScore() == null ? 0.0 : document.getScore();
        Map<String, Object> metadata = document.getMetadata() == null ? Map.of() : document.getMetadata();
        return new DocumentSearchHit(id, document.getText(), metadata, similarity);
    }
}
