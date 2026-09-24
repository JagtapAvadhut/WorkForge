package com.avadhoot.workforge.issue.dto;

import com.avadhoot.workforge.issue.domain.Comment;
import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.dto.UserResponse;

import java.time.Instant;

/**
 * Comment payload aligned with the frontend {@code Comment} model: nested author
 * and the owning issue key.
 */
public record CommentResponse(
        String id,
        String issueKey,
        UserResponse author,
        String body,
        Instant createdAt,
        Instant updatedAt,
        boolean edited
) {
    public static CommentResponse from(Comment c, String issueKey, User author) {
        boolean edited = c.getUpdatedAt() != null && c.getCreatedAt() != null
                && c.getUpdatedAt().isAfter(c.getCreatedAt());
        return new CommentResponse(
                String.valueOf(c.getId()),
                issueKey,
                UserResponse.from(author),
                c.getBody(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                edited);
    }
}
