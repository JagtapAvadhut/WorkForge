package com.avadhoot.workforgeai.ai.memory;

import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import com.avadhoot.workforgeai.ai.memory.repository.MemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Explicit long-term memory only — never auto-stores every chat message.
 */
@Service
public class MemoryService {

    private final MemoryRepository memoryRepository;
    private final int maxRetrieve;

    public MemoryService(
            MemoryRepository memoryRepository,
            @Value("${workforge.ai.memory.max-retrieve:5}") int maxRetrieve) {
        this.memoryRepository = memoryRepository;
        this.maxRetrieve = Math.max(1, maxRetrieve);
    }

    public MemoryRecord remember(String sessionId, String category, String content, Integer importance) {
        String session = ConversationService.normalizeSession(sessionId);
        if (!StringUtils.hasText(category)) {
            throw new IllegalArgumentException("category is required");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("content is required");
        }
        String cat = category.trim();
        if (cat.length() > 64) {
            throw new IllegalArgumentException("category must be at most 64 characters");
        }
        String body = content.trim();
        if (body.length() > 4000) {
            throw new IllegalArgumentException("content must be at most 4000 characters");
        }
        int imp = importance == null ? 3 : importance;
        if (imp < 1 || imp > 10) {
            throw new IllegalArgumentException("importance must be between 1 and 10");
        }
        return memoryRepository.insert(session, cat, body, "explicit", imp);
    }

    public MemoryRecord getRequired(UUID id) {
        return memoryRepository.findById(id)
                .filter(MemoryRecord::active)
                .orElseThrow(() -> new IllegalArgumentException("Memory not found: " + id));
    }

    public List<MemoryRecord> list(String sessionId, String category, Integer minImportance) {
        return memoryRepository.list(
                ConversationService.normalizeSession(sessionId),
                category,
                minImportance,
                100);
    }

    public List<MemoryRecord> retrieveRelevant(String sessionId) {
        return memoryRepository.list(
                ConversationService.normalizeSession(sessionId),
                null,
                null,
                maxRetrieve);
    }

    public void delete(UUID id) {
        if (!memoryRepository.softDelete(id) && !memoryRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Memory not found: " + id);
        }
    }

    public String formatForPrompt(List<MemoryRecord> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("Known user preferences / long-term notes:\n");
        for (MemoryRecord memory : memories) {
            sb.append("- [").append(memory.category()).append(" | importance=")
                    .append(memory.importance()).append("] ")
                    .append(memory.content()).append('\n');
        }
        return sb.toString().trim();
    }
}
