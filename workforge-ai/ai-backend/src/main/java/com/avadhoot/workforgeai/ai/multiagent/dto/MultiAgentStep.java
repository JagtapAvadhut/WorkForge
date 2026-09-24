package com.avadhoot.workforgeai.ai.multiagent.dto;

import java.util.Map;

public record MultiAgentStep(
        String node,
        String action,
        Map<String, Object> details
) {
}
