package com.avadhoot.workforgeai.ai.agent;

import java.util.Map;

/**
 * One decide/act/observe step in the agent loop.
 */
public record AgentStep(
        int stepNumber,
        String thought,
        String action,
        String tool,
        Map<String, Object> arguments,
        String observation,
        String status
) {
}
