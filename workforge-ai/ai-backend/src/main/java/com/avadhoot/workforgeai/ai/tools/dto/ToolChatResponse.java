package com.avadhoot.workforgeai.ai.tools.dto;

import java.util.List;

public record ToolChatResponse(
        String response,
        List<ToolCallInfo> toolCalls
) {
}
