package com.avadhoot.workforgeai.ai.document.service;

import com.avadhoot.workforgeai.ai.document.dto.CreateDocumentRequest;
import com.avadhoot.workforgeai.ai.document.dto.DocumentResponse;
import com.avadhoot.workforgeai.ai.document.model.AiDocumentRecord;
import com.avadhoot.workforgeai.ai.document.repository.AiDocumentRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ingests raw text: validate → embed (via VectorStore/EmbeddingModel) → persist in PGVector.
 */
@Service
public class DocumentIngestionService {

    private final VectorStore vectorStore;
    private final AiDocumentRepository repository;
    private final int embeddingDimensions;

    public DocumentIngestionService(
            VectorStore vectorStore,
            AiDocumentRepository repository,
            @Value("${workforge.ai.embeddings.dimensions:768}") int embeddingDimensions) {
        this.vectorStore = vectorStore;
        this.repository = repository;
        this.embeddingDimensions = embeddingDimensions;
    }

    public DocumentResponse ingest(CreateDocumentRequest request) {
        if (request == null || !StringUtils.hasText(request.content())) {
            throw new IllegalArgumentException("content must not be blank");
        }

        UUID id = UUID.randomUUID();
        Map<String, Object> metadata = sanitizeMetadata(request.metadata());

        Document document = Document.builder()
                .id(id.toString())
                .text(request.content().trim())
                .metadata(metadata)
                .build();

        vectorStore.add(List.of(document));

        AiDocumentRecord saved = repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Document was not persisted: " + id));

        return toResponse(saved);
    }

    public void delete(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Document not found: " + id);
        }
        vectorStore.delete(List.of(id.toString()));
    }

    DocumentResponse toResponse(AiDocumentRecord record) {
        int dims = record.embeddingDimensions() > 0 ? record.embeddingDimensions() : embeddingDimensions;
        return new DocumentResponse(
                record.id(),
                record.content(),
                record.metadata() == null ? Map.of() : record.metadata(),
                dims,
                record.createdAt(),
                record.updatedAt());
    }

    private static Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return new HashMap<>(metadata);
    }
}
