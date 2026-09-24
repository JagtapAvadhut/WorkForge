package com.avadhoot.workforgeai.ai.prompt;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Prompt engineering strategies available to the chat API.
 */
public enum PromptStrategy {
    GENERAL,
    DOMAIN_EXPERT,
    CONCISE,
    DETAILED,
    FEW_SHOT,
    STRUCTURED;

    @JsonCreator
    public static PromptStrategy from(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        try {
            return PromptStrategy.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid strategy '" + raw + "'. Allowed: GENERAL, DOMAIN_EXPERT, CONCISE, DETAILED, FEW_SHOT, STRUCTURED");
        }
    }
}
