package com.avadhoot.workforgeai.ai.observability;

/**
 * Optional cost calculator. Local Ollama typically does not expose billable cost —
 * this remains pluggable and returns null when usage is unknown.
 */
public interface TokenCostCalculator {

    /**
     * @return estimated cost in currency units, or null when unknown
     */
    Double estimateCost(Integer inputTokens, Integer outputTokens, Integer totalTokens);
}
