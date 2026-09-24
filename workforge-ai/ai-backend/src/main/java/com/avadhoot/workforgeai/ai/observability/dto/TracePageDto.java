package com.avadhoot.workforgeai.ai.observability.dto;

import java.util.List;

public record TracePageDto(
        List<TraceDto> items,
        long total,
        int limit,
        int offset
) {
}
