package com.avadhoot.workforge.issue.dto;

import com.avadhoot.workforge.issue.domain.Component;
import com.avadhoot.workforge.issue.domain.Label;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class LabelComponentDtos {

    private LabelComponentDtos() {
    }

    public record CreateLabelRequest(
            @NotNull Long projectId,
            @NotBlank @Size(max = 50) String name,
            @Size(max = 20) String color) {
    }

    public record LabelResponse(Long id, Long projectId, String name, String color) {
        public static LabelResponse from(Label l) {
            return new LabelResponse(l.getId(), l.getProjectId(), l.getName(), l.getColor());
        }
    }

    public record CreateComponentRequest(
            @NotNull Long projectId,
            @NotBlank @Size(max = 100) String name,
            @Size(max = 500) String description,
            Long leadId) {
    }

    public record ComponentResponse(Long id, Long projectId, String name, String description, Long leadId) {
        public static ComponentResponse from(Component c) {
            return new ComponentResponse(c.getId(), c.getProjectId(), c.getName(),
                    c.getDescription(), c.getLeadId());
        }
    }
}
