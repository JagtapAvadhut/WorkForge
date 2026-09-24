package com.avadhoot.workforgeai.ai.document;

import com.avadhoot.workforgeai.ai.document.dto.CreateDocumentRequest;
import com.avadhoot.workforgeai.ai.document.dto.DocumentPageResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentResponse;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchHit;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchRequest;
import com.avadhoot.workforgeai.ai.document.dto.DocumentSearchResponse;
import com.avadhoot.workforgeai.ai.document.model.AiDocumentRecord;
import com.avadhoot.workforgeai.ai.document.repository.AiDocumentRepository;
import com.avadhoot.workforgeai.ai.document.service.DocumentIngestionService;
import com.avadhoot.workforgeai.ai.document.service.DocumentQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    private VectorStore vectorStore;
    private AiDocumentRepository repository;
    private DocumentIngestionService ingestionService;
    private DocumentQueryService queryService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        repository = mock(AiDocumentRepository.class);
        ingestionService = new DocumentIngestionService(vectorStore, repository, 768);
        queryService = new DocumentQueryService(vectorStore, repository, ingestionService, 20, 5, 20, 100);
    }

    @Test
    void ingest_persistsViaVectorStore() {
        when(repository.findById(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return Optional.of(new AiDocumentRecord(
                    id,
                    "A sprint is a fixed development period.",
                    Map.of("topic", "sprint"),
                    768,
                    Instant.parse("2026-01-01T00:00:00Z"),
                    Instant.parse("2026-01-01T00:00:00Z")));
        });

        DocumentResponse response = ingestionService.ingest(new CreateDocumentRequest(
                "A sprint is a fixed development period.",
                Map.of("topic", "sprint")));

        assertThat(response.embeddingDimensions()).isEqualTo(768);
        assertThat(response.content()).contains("sprint");
        verify(vectorStore).add(anyList());
    }

    @Test
    void ingest_rejectsBlankContent() {
        assertThatThrownBy(() -> ingestionService.ingest(new CreateDocumentRequest("  ", Map.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    void search_ranksBySimilarityDescending() {
        Document high = Document.builder()
                .id(UUID.randomUUID().toString())
                .text("A sprint is a fixed development period.")
                .metadata(Map.of("topic", "sprint"))
                .score(0.91)
                .build();
        Document low = Document.builder()
                .id(UUID.randomUUID().toString())
                .text("A bug describes incorrect system behavior.")
                .metadata(Map.of("topic", "bug"))
                .score(0.22)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(low, high));

        DocumentSearchResponse response = queryService.search(
                new DocumentSearchRequest("How does a sprint work?", 5));

        assertThat(response.results()).extracting(DocumentSearchHit::similarity)
                .containsExactly(0.91, 0.22);
        assertThat(response.results().getFirst().content()).contains("sprint");
    }

    @Test
    void search_emptyDatabaseReturnsEmptyResults() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        DocumentSearchResponse response = queryService.search(
                new DocumentSearchRequest("anything", 3));

        assertThat(response.results()).isEmpty();
        assertThat(response.topK()).isEqualTo(3);
    }

    @Test
    void search_rejectsBlankQueryAndInvalidTopK() {
        assertThatThrownBy(() -> queryService.search(new DocumentSearchRequest("  ", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("query");
        assertThatThrownBy(() -> queryService.search(new DocumentSearchRequest("q", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> queryService.search(new DocumentSearchRequest("q", 99)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void list_supportsPagination() {
        when(repository.count()).thenReturn(25L);
        when(repository.findPage(anyInt(), anyInt())).thenReturn(List.of(
                new AiDocumentRecord(
                        UUID.randomUUID(),
                        "doc",
                        Map.of(),
                        768,
                        Instant.now(),
                        Instant.now())));

        DocumentPageResponse page = queryService.list(1, 10);

        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(25);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.items()).hasSize(1);
    }

    @Test
    void delete_requiresExistingDocument() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);
        assertThatThrownBy(() -> ingestionService.delete(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        when(repository.existsById(id)).thenReturn(true);
        ingestionService.delete(id);
        verify(vectorStore).delete(List.of(id.toString()));
    }
}
