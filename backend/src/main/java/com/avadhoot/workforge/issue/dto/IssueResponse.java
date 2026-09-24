package com.avadhoot.workforge.issue.dto;

import com.avadhoot.workforge.issue.dto.RefDtos.ComponentDto;
import com.avadhoot.workforge.issue.dto.RefDtos.LabelDto;
import com.avadhoot.workforge.issue.dto.RefDtos.StatusDto;
import com.avadhoot.workforge.user.dto.UserResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Issue payload aligned with the frontend {@code Issue} model: keys instead of
 * numeric ids in the path contract, nested status/assignee/reporter objects,
 * and type/priority expressed as codes.
 */
public record IssueResponse(
        String id,
        String key,
        String projectKey,
        String projectName,
        String type,
        String summary,
        String description,
        StatusDto status,
        String priority,
        UserResponse assignee,
        UserResponse reporter,
        String sprintId,
        String sprintName,
        List<LabelDto> labels,
        List<ComponentDto> components,
        Integer storyPoints,
        LocalDate dueDate,
        String rank,
        Instant createdAt,
        Instant updatedAt,
        Integer commentCount
) {
}
