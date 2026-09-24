package com.avadhoot.workforgeai.ai.memory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RememberRequest(
        @Size(max = 128) String sessionId,

        @NotBlank
        @Size(max = 64)
        String category,

        @NotBlank
        @Size(max = 4000)
        String content,

        @Min(1)
        @Max(10)
        Integer importance
) {
}
