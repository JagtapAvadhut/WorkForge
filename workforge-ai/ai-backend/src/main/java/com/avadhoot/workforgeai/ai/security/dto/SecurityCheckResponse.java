package com.avadhoot.workforgeai.ai.security.dto;

public record SecurityCheckResponse(
        String status,
        String reason,
        boolean allowed
) {
}
