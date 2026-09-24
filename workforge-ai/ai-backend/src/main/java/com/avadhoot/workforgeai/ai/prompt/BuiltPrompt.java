package com.avadhoot.workforgeai.ai.prompt;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Fully rendered prompt ready for ChatClient or preview.
 */
public record BuiltPrompt(
        PromptStrategy strategy,
        PromptVersion version,
        String systemPrompt,
        List<Message> contextMessages,
        String userPrompt
) {
}
