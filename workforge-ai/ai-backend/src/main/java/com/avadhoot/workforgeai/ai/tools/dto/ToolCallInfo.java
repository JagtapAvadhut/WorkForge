package com.avadhoot.workforgeai.ai.tools.dto;

import java.util.List;
import java.util.Map;

public record ToolCallInfo(
        String tool,
        Map<String, Object> arguments,
        String resultSummary
) {
}
