package com.avadhoot.workforge.auth.dto;

import com.avadhoot.workforge.user.dto.UserResponse;

import java.util.Set;

public record MeResponse(
        UserResponse user,
        Set<String> permissions
) {
}
