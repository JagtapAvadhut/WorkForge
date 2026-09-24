package com.avadhoot.workforgeai.ai.memory;

import com.avadhoot.workforgeai.ai.dto.HistoryMessage;
import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MessageRecord;
import com.avadhoot.workforgeai.ai.memory.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    public static final String DEFAULT_SESSION = "default";

    private final ConversationRepository conversationRepository;
    private final int maxRecentMessages;

    public ConversationService(
            ConversationRepository conversationRepository,
            @Value("${workforge.ai.memory.max-recent-messages:10}") int maxRecentMessages) {
        this.conversationRepository = conversationRepository;
        this.maxRecentMessages = Math.max(1, maxRecentMessages);
    }

    public ConversationRecord create(String sessionId, String title) {
        String session = normalizeSession(sessionId);
        String safeTitle = StringUtils.hasText(title) ? title.trim() : "New conversation";
        if (safeTitle.length() > 300) {
            safeTitle = safeTitle.substring(0, 300);
        }
        return conversationRepository.insert(session, safeTitle);
    }

    public ConversationRecord getRequired(UUID id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + id));
    }

    public ConversationRecord resolveOrCreate(String conversationId, String sessionId, String firstMessage) {
        String session = normalizeSession(sessionId);
        if (StringUtils.hasText(conversationId)) {
            UUID id = parseUuid(conversationId, "conversationId");
            ConversationRecord existing = getRequired(id);
            if (!existing.sessionId().equals(session)) {
                throw new IllegalArgumentException("Conversation does not belong to this session");
            }
            return existing;
        }
        String title = deriveTitle(firstMessage);
        return conversationRepository.insert(session, title);
    }

    public List<ConversationRecord> list(String sessionId, int limit) {
        return conversationRepository.listBySession(normalizeSession(sessionId), Math.min(Math.max(limit, 1), 100));
    }

    public List<MessageRecord> listMessages(UUID conversationId) {
        getRequired(conversationId);
        return conversationRepository.listMessages(conversationId);
    }

    public List<MessageRecord> recentMessages(UUID conversationId) {
        getRequired(conversationId);
        return conversationRepository.listRecentMessages(conversationId, maxRecentMessages);
    }

    public List<HistoryMessage> recentAsHistory(UUID conversationId) {
        return recentMessages(conversationId).stream()
                .map(m -> new HistoryMessage(m.role(), m.content()))
                .toList();
    }

    public MessageRecord saveMessage(UUID conversationId, String role, String content) {
        getRequired(conversationId);
        if (!StringUtils.hasText(role) || !List.of("user", "assistant", "system").contains(role)) {
            throw new IllegalArgumentException("role must be user, assistant, or system");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content must not be blank");
        }
        String trimmed = content.trim();
        if (trimmed.length() > 20_000) {
            trimmed = trimmed.substring(0, 20_000);
        }
        return conversationRepository.insertMessage(conversationId, role, trimmed);
    }

    public void delete(UUID conversationId) {
        if (!conversationRepository.delete(conversationId)) {
            throw new IllegalArgumentException("Conversation not found: " + conversationId);
        }
    }

    public int maxRecentMessages() {
        return maxRecentMessages;
    }

    public static String normalizeSession(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            return DEFAULT_SESSION;
        }
        String trimmed = sessionId.trim();
        if (trimmed.length() > 128) {
            throw new IllegalArgumentException("sessionId must be at most 128 characters");
        }
        return trimmed;
    }

    public static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(value.trim());
        } catch (Exception ex) {
            throw new IllegalArgumentException(field + " must be a valid UUID");
        }
    }

    private static String deriveTitle(String message) {
        if (!StringUtils.hasText(message)) {
            return "New conversation";
        }
        String trimmed = message.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= 80) {
            return trimmed;
        }
        return trimmed.substring(0, 77) + "...";
    }
}
