package com.avadhoot.workforgeai.ai.dto;

import com.avadhoot.workforgeai.ai.prompt.PromptStrategy;
import com.avadhoot.workforgeai.ai.prompt.PromptVersion;

import java.util.List;

public record PromptPreviewResponse(
        PromptStrategy strategy,
        PromptVersion version,
        String systemPrompt,
        List<PreviewMessage> messages,
        String userPrompt
) {
    public record PreviewMessage(String role, String content) {
    }
}
