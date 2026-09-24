package com.avadhoot.workforgeai.ai.rag;

import com.avadhoot.workforgeai.ai.rag.dto.RagQueryRequest;
import com.avadhoot.workforgeai.ai.rag.dto.RagQueryResponse;
import com.avadhoot.workforgeai.ai.rag.dto.RagSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {

    private VectorStore vectorStore;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private RagService ragService;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        ragService = new RagService(
                vectorStore,
                chatClient,
                new RagPromptBuilder(),
                5,
                20,
                0.45,
                "I don't have enough information in the available knowledge.");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
    }

    @Test
    void query_returnsAnswerAndSources_forRelevantHits() {
        UUID id = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder()
                        .id(id.toString())
                        .text("A sprint is a fixed development period.")
                        .metadata(Map.of("topic", "sprint"))
                        .score(0.82)
                        .build()));
        when(callSpec.content()).thenReturn("A sprint is a fixed development period used by a team.");

        RagQueryResponse response = ragService.query(new RagQueryRequest("What is a sprint?", 5));

        assertThat(response.answer()).contains("sprint");
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().getFirst().id()).isEqualTo(id);
        assertThat(response.sources().getFirst().similarity()).isEqualTo(0.82);
        verify(chatClient).prompt();
    }

    @Test
    void query_skipsLlm_whenNoDocumentsPassThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder()
                        .id(UUID.randomUUID().toString())
                        .text("unrelated")
                        .score(0.10)
                        .build()));

        RagQueryResponse response = ragService.query(new RagQueryRequest("quantum teleportation recipes", 5));

        assertThat(response.answer()).isEqualTo("I don't have enough information in the available knowledge.");
        assertThat(response.sources()).isEmpty();
        verify(chatClient, never()).prompt();
    }

    @Test
    void query_emptyStore_returnsNoContext() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        RagQueryResponse response = ragService.query(new RagQueryRequest("What is a board?", 3));

        assertThat(response.sources()).isEmpty();
        assertThat(response.answer()).contains("enough information");
        verify(chatClient, never()).prompt();
    }

    @Test
    void retrieve_removesDuplicateIds() {
        UUID id = UUID.randomUUID();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder().id(id.toString()).text("A").score(0.9).build(),
                Document.builder().id(id.toString()).text("A duplicate").score(0.8).build(),
                Document.builder().id(UUID.randomUUID().toString()).text("B").score(0.7).build()));

        List<RagSource> sources = ragService.retrieve("question", 5);

        assertThat(sources).hasSize(2);
        assertThat(sources.getFirst().id()).isEqualTo(id);
        assertThat(sources.getFirst().content()).isEqualTo("A");
    }

    @Test
    void retrieve_filtersBySimilarityThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder().id(UUID.randomUUID().toString()).text("high").score(0.70).build(),
                Document.builder().id(UUID.randomUUID().toString()).text("low").score(0.20).build()));

        List<RagSource> sources = ragService.retrieve("question", 5);

        assertThat(sources).hasSize(1);
        assertThat(sources.getFirst().content()).isEqualTo("high");
        assertThat(ragService.similarityThreshold()).isEqualTo(0.45);
    }

    @Test
    void query_rejectsBlankAndInvalidTopK() {
        assertThatThrownBy(() -> ragService.query(new RagQueryRequest("  ", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("question");
        assertThatThrownBy(() -> ragService.query(new RagQueryRequest("q", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("topK");
        assertThatThrownBy(() -> ragService.query(new RagQueryRequest("q", 99)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
    }

    @Test
    void promptBuilder_includesContextAndQuestion() {
        RagPromptBuilder builder = new RagPromptBuilder();
        String user = builder.userPrompt(
                "What is a sprint?",
                List.of(new RagSource(
                        UUID.randomUUID(),
                        "A sprint is a fixed development period.",
                        Map.of("topic", "sprint"),
                        0.9)));

        assertThat(user).contains("CONTEXT:");
        assertThat(user).contains("A sprint is a fixed development period.");
        assertThat(user).contains("QUESTION:");
        assertThat(user).contains("What is a sprint?");
        assertThat(builder.systemPrompt()).contains("ONLY the supplied CONTEXT");
    }

    @Test
    void topK_defaultsWhenNull() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        RagQueryResponse response = ragService.query(new RagQueryRequest("What is priority?", null));
        assertThat(response.answer()).contains("enough information");
    }
}
