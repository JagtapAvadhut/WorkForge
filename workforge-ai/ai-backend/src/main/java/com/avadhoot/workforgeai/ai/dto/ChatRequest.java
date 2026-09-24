package com.avadhoot.workforgeai.ai.dto;

import com.avadhoot.workforgeai.ai.prompt.PromptStrategy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ChatRequest(
        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be at most 4000 characters")
        String message,

        PromptStrategy strategy,

        @Size(max = 50, message = "history must contain at most 50 messages")
        List<@Valid HistoryMessage> history,

        @Size(max = 64) String conversationId,

        @Size(max = 128) String sessionId
) {
    public ChatRequest(String message) {
        this(message, PromptStrategy.GENERAL, List.of(), null, null);
    }

    public ChatRequest(String message, PromptStrategy strategy, List<HistoryMessage> history) {
        this(message, strategy, history, null, null);
    }

    public PromptStrategy resolvedStrategy() {
        return strategy == null ? PromptStrategy.GENERAL : strategy;
    }
}
