package com.avadhoot.workforgeai.ai.memory;

import com.avadhoot.workforgeai.ai.memory.dto.ConversationDto;
import com.avadhoot.workforgeai.ai.memory.dto.CreateConversationRequest;
import com.avadhoot.workforgeai.ai.memory.dto.MemoryDto;
import com.avadhoot.workforgeai.ai.memory.dto.MessageDto;
import com.avadhoot.workforgeai.ai.memory.dto.RememberRequest;
import com.avadhoot.workforgeai.ai.memory.model.ConversationRecord;
import com.avadhoot.workforgeai.ai.memory.model.MemoryRecord;
import com.avadhoot.workforgeai.ai.memory.model.MessageRecord;
import com.avadhoot.workforgeai.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/memory")
public class MemoryController {

    private final ConversationService conversationService;
    private final MemoryService memoryService;

    public MemoryController(ConversationService conversationService, MemoryService memoryService) {
        this.conversationService = conversationService;
        this.memoryService = memoryService;
    }

    @PostMapping("/conversations")
    public ApiResponse<ConversationDto> createConversation(@Valid @RequestBody(required = false) CreateConversationRequest request) {
        CreateConversationRequest body = request == null ? new CreateConversationRequest(null, null) : request;
        return ApiResponse.ok(toConversation(conversationService.create(body.sessionId(), body.title())));
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ConversationDto>> listConversations(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(conversationService.list(sessionId, limit).stream().map(this::toConversation).toList());
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<ConversationDto> getConversation(@PathVariable UUID id) {
        return ApiResponse.ok(toConversation(conversationService.getRequired(id)));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageDto>> listMessages(@PathVariable UUID id) {
        return ApiResponse.ok(conversationService.listMessages(id).stream().map(this::toMessage).toList());
    }

    @DeleteMapping("/conversations/{id}")
    public ApiResponse<Map<String, Object>> deleteConversation(@PathVariable UUID id) {
        conversationService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id.toString()));
    }

    @PostMapping("/remember")
    public ApiResponse<MemoryDto> remember(@Valid @RequestBody RememberRequest request) {
        return ApiResponse.ok(toMemory(memoryService.remember(
                request.sessionId(),
                request.category(),
                request.content(),
                request.importance())));
    }

    @GetMapping
    public ApiResponse<List<MemoryDto>> listMemories(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer minImportance) {
        return ApiResponse.ok(memoryService.list(sessionId, category, minImportance).stream()
                .map(this::toMemory)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<MemoryDto> getMemory(@PathVariable UUID id) {
        return ApiResponse.ok(toMemory(memoryService.getRequired(id)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteMemory(@PathVariable UUID id) {
        memoryService.delete(id);
        return ApiResponse.ok(Map.of("deleted", true, "id", id.toString()));
    }

    private ConversationDto toConversation(ConversationRecord record) {
        return new ConversationDto(
                record.id(),
                record.sessionId(),
                record.title(),
                record.createdAt(),
                record.updatedAt());
    }

    private MessageDto toMessage(MessageRecord record) {
        return new MessageDto(
                record.id(),
                record.conversationId(),
                record.role(),
                record.content(),
                record.createdAt());
    }

    private MemoryDto toMemory(MemoryRecord record) {
        return new MemoryDto(
                record.id(),
                record.sessionId(),
                record.category(),
                record.content(),
                record.source(),
                record.importance(),
                record.createdAt(),
                record.updatedAt());
    }
}
