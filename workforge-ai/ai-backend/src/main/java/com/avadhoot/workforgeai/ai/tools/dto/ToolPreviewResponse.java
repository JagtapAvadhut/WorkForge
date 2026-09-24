package com.avadhoot.workforgeai.ai.tools.dto;

import java.util.List;

public record ToolPreviewResponse(
        String message,
        List<ToolDefinitionView> tools
) {
    public record ToolDefinitionView(
            String name,
            String description,
            String inputSchema
    ) {
    }
}
