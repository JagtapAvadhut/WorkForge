package com.avadhoot.workforge.user.dto;

import com.avadhoot.workforge.common.CodeMappings;
import com.avadhoot.workforge.user.domain.Role;
import com.avadhoot.workforge.user.domain.User;

import java.time.Instant;
import java.util.List;

/**
 * User payload aligned with the frontend {@code User} model. IDs are serialised
 * as strings and RBAC roles are mapped to the coarse frontend role vocabulary.
 */
public record UserResponse(
        String id,
        String email,
        String username,
        String fullName,
        String displayName,
        String avatarUrl,
        List<String> roles,
        boolean active,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        if (user == null) {
            return null;
        }
        List<String> roles = user.getRoles() == null ? List.of()
                : user.getRoles().stream()
                    .map(Role::getName)
                    .map(CodeMappings::frontendRole)
                    .distinct()
                    .sorted()
                    .toList();
        String display = (user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName() : user.getUsername();
        return new UserResponse(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                display,
                user.getAvatarUrl(),
                roles,
                user.isEnabled(),
                user.getCreatedAt());
    }
}
