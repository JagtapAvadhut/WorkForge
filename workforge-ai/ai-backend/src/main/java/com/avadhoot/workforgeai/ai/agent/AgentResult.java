package com.avadhoot.workforgeai.ai.agent;

import java.util.List;

/**
 * Internal result of a completed agent run.
 */
public record AgentResult(
        String answer,
        List<AgentStep> steps,
        String stopReason,
        int iterations
) {
}
