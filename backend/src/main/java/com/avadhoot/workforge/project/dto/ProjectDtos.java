package com.avadhoot.workforge.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.avadhoot.workforge.user.dto.UserResponse;

import java.time.Instant;

public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record CreateProjectRequest(
            @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$",
                    message = "Key must be 2-10 uppercase alphanumerics starting with a letter") String key,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 1000) String description,
            String leadId,
            String organizationId) {
    }

    public record UpdateProjectRequest(
            @Size(max = 150) String name,
            @Size(max = 1000) String description,
            String leadId,
            Boolean enabled) {
    }

    /** Aligned with the frontend {@code Project} model. */
    public record ProjectResponse(
            String id,
            String key,
            String name,
            String description,
            UserResponse lead,
            Integer memberCount,
            Integer issueCount,
            Integer openIssueCount,
            String avatarColor,
            Instant createdAt) {
    }

    public record AddMemberRequest(
            @NotBlank String userId,
            @NotBlank String role) {
    }

    /** Aligned with the frontend {@code ProjectMember} model. */
    public record ProjectMemberResponse(UserResponse user, String role, Instant joinedAt) {
    }
}
