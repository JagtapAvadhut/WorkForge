package com.avadhoot.workforge.issue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Issue request payloads aligned with the frontend contract: projects and
 * reference data are addressed by key / code, and ids arrive as strings.
 */
public final class IssueDtos {

    private IssueDtos() {
    }

    public record CreateIssueRequest(
            String projectKey,
            @NotBlank @Size(max = 255) String summary,
            String description,
            String type,
            String priority,
            String assigneeId,
            String parentId,
            String sprintId,
            Integer storyPoints,
            LocalDate dueDate,
            List<String> labelIds,
            List<String> componentIds) {
    }

    public record UpdateIssueRequest(
            @Size(max = 255) String summary,
            String description,
            String type,
            String priority,
            String assigneeId,
            String sprintId,
            String statusId,
            Integer storyPoints,
            LocalDate dueDate,
            List<String> labelIds,
            List<String> componentIds) {
    }

    public record AssigneePatch(String assigneeId) {
    }

    public record StatusPatch(@NotBlank String statusId, String rank) {
    }

    public record PriorityPatch(String priority, String priorityId) {
        public String resolved() {
            return priority != null ? priority : priorityId;
        }
    }

    public record SprintPatch(String sprintId) {
    }

    public record CommentRequest(@NotBlank String body) {
    }
}
