package com.avadhoot.workforgeai.ai.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SecurityCheckRequest(
        @NotBlank(message = "input is required")
        @Size(max = 8000, message = "input must be at most 8000 characters")
        String input
) {
}
