package com.avadhoot.workforgeai.ai.evaluation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EvaluationRunRequest(
        @NotBlank(message = "suite is required")
        @Size(max = 64)
        String suite
) {
    public EvaluationRunRequest {
        if (suite == null || suite.isBlank()) {
            suite = "ALL";
        } else {
            suite = suite.trim().toUpperCase();
        }
    }
}
