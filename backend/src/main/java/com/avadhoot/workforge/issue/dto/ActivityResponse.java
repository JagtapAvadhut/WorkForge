package com.avadhoot.workforge.issue.dto;

import com.avadhoot.workforge.user.dto.UserResponse;

import java.time.Instant;

/**
 * Activity feed entry aligned with the frontend {@code ActivityEntry} model.
 */
public record ActivityResponse(
        String id,
        String type,
        UserResponse actor,
        String field,
        String from,
        String to,
        Instant createdAt
) {
}
