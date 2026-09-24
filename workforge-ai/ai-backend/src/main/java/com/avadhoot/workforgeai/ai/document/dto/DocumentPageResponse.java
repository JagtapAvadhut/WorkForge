package com.avadhoot.workforgeai.ai.document.dto;

import java.util.List;

public record DocumentPageResponse(
        List<DocumentResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
