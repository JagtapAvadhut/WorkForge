package com.avadhoot.workforge.user.dto;

import com.avadhoot.workforge.user.domain.RoleName;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        @Size(max = 500) String avatarUrl,
        Set<RoleName> roles
) {
}
