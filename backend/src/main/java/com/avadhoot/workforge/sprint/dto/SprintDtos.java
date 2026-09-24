package com.avadhoot.workforge.sprint.dto;

import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.issue.dto.IssueResponse;
import com.avadhoot.workforge.sprint.domain.Sprint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SprintDtos {

    private SprintDtos() {
    }

    public record CreateSprintRequest(
            Long projectId,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 1000) String goal) {
    }

    public record CreateSprintByKeyRequest(
            @NotBlank @Size(max = 150) String name,
            @Size(max = 1000) String goal,
            Instant startDate,
            Instant endDate) {
    }

    public record UpdateSprintRequest(
            @Size(max = 150) String name,
            @Size(max = 1000) String goal,
            Instant startDate,
            Instant endDate) {
    }

    public record StartSprintRequest(
            Instant startDate,
            Instant endDate) {
    }

    public record CompleteSprintRequest(String moveToSprintId) {
    }

    /**
     * Frontend-aligned sprint payload (string ids, COMPLETED instead of CLOSED).
     */
    public record SprintResponse(
            String id,
            String name,
            String goal,
            String state,
            Instant startDate,
            Instant endDate,
            String projectKey,
            Integer issueCount,
            Integer completedPoints,
            Integer totalPoints) {

        public static SprintResponse from(Sprint s, String projectKey) {
            return new SprintResponse(
                    String.valueOf(s.getId()),
                    s.getName(),
                    s.getGoal(),
                    CodeMappings.sprintStateCode(s.getState()),
                    s.getStartDate(),
                    s.getEndDate(),
                    projectKey,
                    null,
                    null,
                    null);
        }

        public static SprintResponse from(Sprint s, String projectKey, int issueCount) {
            return new SprintResponse(
                    String.valueOf(s.getId()),
                    s.getName(),
                    s.getGoal(),
                    CodeMappings.sprintStateCode(s.getState()),
                    s.getStartDate(),
                    s.getEndDate(),
                    projectKey,
                    issueCount,
                    null,
                    null);
        }
    }

    public record SprintBucket(SprintResponse sprint, List<IssueResponse> issues) {
    }

    public record BacklogData(List<IssueResponse> backlog, List<SprintBucket> sprints) {
    }
}
