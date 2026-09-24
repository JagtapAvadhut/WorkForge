package com.avadhoot.workforgeai.ai.memory;

import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Prepares bounded conversation + long-term memory for a request, then persists the turn.
 * Does not persist agent steps or chain-of-thought.
 */
@Service
public class ConversationMemoryFacade {

    private final ConversationService conversationService;
    private final MemoryService memoryService;

    public ConversationMemoryFacade(ConversationService conversationService, MemoryService memoryService) {
        this.conversationService = conversationService;
        this.memoryService = memoryService;
    }

    public PreparedContext prepare(String conversationId, String sessionId, String userMessage) {
        ConversationRecord conversation = conversationService.resolveOrCreate(
                conversationId, sessionId, userMessage);
        List<HistoryMessage> history = conversationService.recentAsHistory(conversation.id());
        List<MemoryRecord> memories = memoryService.retrieveRelevant(conversation.sessionId());
        String memoryBlock = memoryService.formatForPrompt(memories);
        return new PreparedContext(
                conversation.id(),
                conversation.sessionId(),
                history,
                memories,
                memoryBlock);
    }

    public void completeTurn(UUID conversationId, String userMessage, String assistantMessage) {
        conversationService.saveMessage(conversationId, "user", userMessage);
        if (StringUtils.hasText(assistantMessage)) {
            conversationService.saveMessage(conversationId, "assistant", assistantMessage);
        }
    }

    /**
     * Augments the user message with long-term memory notes (not conversation history —
     * history is passed separately where supported).
     */
    public String augmentUserMessage(String userMessage, String memoryBlock) {
        if (!StringUtils.hasText(memoryBlock)) {
            return userMessage;
        }
        return """
                %s

                Current user request:
                %s
                """.formatted(memoryBlock, userMessage).trim();
    }

    public record PreparedContext(
            UUID conversationId,
            String sessionId,
            List<HistoryMessage> recentHistory,
            List<MemoryRecord> longTermMemories,
            String memoryBlock
    ) {
        public String conversationIdString() {
            return conversationId.toString();
        }
    }
}
