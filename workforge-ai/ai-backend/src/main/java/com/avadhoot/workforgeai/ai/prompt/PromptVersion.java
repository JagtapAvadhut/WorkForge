package com.avadhoot.workforgeai.ai.prompt;

/**
 * Simple in-code prompt versioning (no database yet).
 */
public enum PromptVersion {
    GENERAL_V1(PromptStrategy.GENERAL),
    DOMAIN_EXPERT_V1(PromptStrategy.DOMAIN_EXPERT),
    CONCISE_V1(PromptStrategy.CONCISE),
    DETAILED_V1(PromptStrategy.DETAILED),
    FEW_SHOT_V1(PromptStrategy.FEW_SHOT),
    STRUCTURED_V1(PromptStrategy.STRUCTURED);

    private final PromptStrategy strategy;

    PromptVersion(PromptStrategy strategy) {
        this.strategy = strategy;
    }

    public PromptStrategy strategy() {
        return strategy;
    }

    public static PromptVersion forStrategy(PromptStrategy strategy) {
        for (PromptVersion version : values()) {
            if (version.strategy == strategy) {
                return version;
            }
        }
        throw new IllegalArgumentException("No prompt version for strategy: " + strategy);
    }
}
