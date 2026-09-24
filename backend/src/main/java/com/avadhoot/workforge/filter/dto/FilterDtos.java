package com.avadhoot.workforge.filter.dto;

import com.avadhoot.workforge.filter.domain.SavedFilter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class FilterDtos {

    private FilterDtos() {
    }

    public record CreateFilterRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Size(max = 2000) String query,
            boolean shared) {
    }

    public record FilterResponse(
            String id,
            Long ownerId,
            String name,
            String query,
            @com.fasterxml.jackson.annotation.JsonProperty("jql") String jql,
            boolean shared,
            java.time.Instant createdAt) {
        public static FilterResponse from(SavedFilter f) {
            return new FilterResponse(
                    String.valueOf(f.getId()),
                    f.getOwnerId(),
                    f.getName(),
                    f.getQuery(),
                    f.getQuery(),
                    f.isShared(),
                    f.getCreatedAt());
        }
    }
}
