package com.avadhoot.workforgeai.ai.prompt;

/**
 * Reusable few-shot pair, kept separate from chat business logic.
 */
public record FewShotExample(String user, String assistant) {
}
