package com.avadhoot.workforge.user.dto;

import com.avadhoot.workforge.user.domain.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull UserStatus status
) {
}
