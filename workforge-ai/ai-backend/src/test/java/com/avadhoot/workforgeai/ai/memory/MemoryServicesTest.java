package com.avadhoot.workforgeai.ai.memory;

import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import com.avadhoot.workforgeai.ai.memory.model.MessageRecord;
import com.avadhoot.workforgeai.ai.memory.repository.ConversationRepository;
import com.avadhoot.workforgeai.ai.memory.repository.MemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryServicesTest {

    private ConversationRepository conversationRepository;
    private MemoryRepository memoryRepository;
    private ConversationService conversationService;
    private MemoryService memoryService;
    private ConversationMemoryFacade facade;

    @BeforeEach
    void setUp() {
        conversationRepository = mock(ConversationRepository.class);
        memoryRepository = mock(MemoryRepository.class);
        conversationService = new ConversationService(conversationRepository, 2);
        memoryService = new MemoryService(memoryRepository, 5);
        facade = new ConversationMemoryFacade(conversationService, memoryService);
    }

    @Test
    void createConversation_persists() {
        UUID id = UUID.randomUUID();
        when(conversationRepository.insert(eq("default"), anyString()))
                .thenReturn(new ConversationRecord(id, "default", "Hello", Instant.now(), Instant.now()));
        when(conversationRepository.findById(id)).thenReturn(Optional.of(
                new ConversationRecord(id, "default", "Hello", Instant.now(), Instant.now())));

        var created = conversationService.create(null, "Hello");

        assertThat(created.id()).isEqualTo(id);
        assertThat(created.sessionId()).isEqualTo("default");
    }

    @Test
    void recentMessages_respectLimit() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(
                new ConversationRecord(conversationId, "default", "t", Instant.now(), Instant.now())));
        when(conversationRepository.listRecentMessages(conversationId, 2)).thenReturn(List.of(
                new MessageRecord(UUID.randomUUID(), conversationId, "user", "one", Instant.now()),
                new MessageRecord(UUID.randomUUID(), conversationId, "assistant", "two", Instant.now())));

        assertThat(conversationService.recentMessages(conversationId)).hasSize(2);
        verify(conversationRepository).listRecentMessages(conversationId, 2);
    }

    @Test
    void conversationIsolation_rejectsOtherSession() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(
                new ConversationRecord(conversationId, "session-a", "t", Instant.now(), Instant.now())));

        assertThatThrownBy(() -> conversationService.resolveOrCreate(
                conversationId.toString(), "session-b", "hi"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void rememberAndRetrieve_longTermMemory() {
        UUID id = UUID.randomUUID();
        MemoryRecord record = new MemoryRecord(
                id, "default", "preference", "User prefers simple explanations",
                "explicit", 5, true, Instant.now(), Instant.now());
        when(memoryRepository.insert(eq("default"), eq("preference"), anyString(), eq("explicit"), eq(5)))
                .thenReturn(record);
        when(memoryRepository.list(eq("default"), eq(null), eq(null), eq(5)))
                .thenReturn(List.of(record));

        memoryService.remember(null, "preference", "User prefers simple explanations", 5);
        String block = memoryService.formatForPrompt(memoryService.retrieveRelevant(null));

        assertThat(block).contains("simple explanations");
        assertThat(block).contains("preference");
    }

    @Test
    void deleteMemory_softDeletes() {
        UUID id = UUID.randomUUID();
        when(memoryRepository.softDelete(id)).thenReturn(true);

        memoryService.delete(id);

        verify(memoryRepository).softDelete(id);
    }

    @Test
    void facade_prepareAndComplete_doesNotStoreChainOfThought() {
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.insert(eq("default"), anyString()))
                .thenReturn(new ConversationRecord(conversationId, "default", "RAG?", Instant.now(), Instant.now()));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(
                new ConversationRecord(conversationId, "default", "RAG?", Instant.now(), Instant.now())));
        when(conversationRepository.listRecentMessages(eq(conversationId), anyInt())).thenReturn(List.of());
        when(memoryRepository.list(eq("default"), eq(null), eq(null), anyInt())).thenReturn(List.of());
        when(conversationRepository.insertMessage(eq(conversationId), eq("user"), anyString()))
                .thenReturn(new MessageRecord(UUID.randomUUID(), conversationId, "user", "What is RAG?", Instant.now()));
        when(conversationRepository.insertMessage(eq(conversationId), eq("assistant"), anyString()))
                .thenReturn(new MessageRecord(UUID.randomUUID(), conversationId, "assistant", "RAG is...", Instant.now()));

        var prepared = facade.prepare(null, "default", "What is RAG?");
        facade.completeTurn(prepared.conversationId(), "What is RAG?", "RAG is retrieval-augmented generation.");

        verify(conversationRepository).insertMessage(conversationId, "user", "What is RAG?");
        verify(conversationRepository).insertMessage(
                conversationId, "assistant", "RAG is retrieval-augmented generation.");
        // Only user/assistant roles — no thought/step persistence
        verify(conversationRepository, org.mockito.Mockito.times(2))
                .insertMessage(any(), anyString(), anyString());
    }
}
