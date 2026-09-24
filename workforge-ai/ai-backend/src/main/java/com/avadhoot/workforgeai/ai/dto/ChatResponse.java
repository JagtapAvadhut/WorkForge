package com.avadhoot.workforgeai.ai.dto;

import com.avadhoot.workforgeai.ai.prompt.PromptStrategy;

public record ChatResponse(
        String response,
        PromptStrategy strategy,
        String conversationId
) {
    public ChatResponse(String response, PromptStrategy strategy) {
        this(response, strategy, null);
    }
}
