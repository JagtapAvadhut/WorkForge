package com.avadhoot.workforgeai.ai.mcp.dto;

import java.util.List;
import java.util.Map;

public record McpChatResponse(
        String response,
        List<Map<String, Object>> toolsUsed
) {
}
