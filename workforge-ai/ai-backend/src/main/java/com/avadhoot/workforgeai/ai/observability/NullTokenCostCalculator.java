package com.avadhoot.workforgeai.ai.observability;

import org.springframework.stereotype.Component;

/**
 * Default: no invented pricing. Returns null unless a future provider supplies rates.
 */
@Component
public class NullTokenCostCalculator implements TokenCostCalculator {

    @Override
    public Double estimateCost(Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        return null;
    }
}
