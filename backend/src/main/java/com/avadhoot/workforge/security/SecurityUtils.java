package com.avadhoot.workforge.security;

import com.avadhoot.workforge.common.ErrorCode;
import com.avadhoot.workforge.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentPrincipal().map(UserPrincipal::getId);
    }

    public static Long requireCurrentUserId() {
        return currentUserId().orElseThrow(
                () -> new BusinessException(ErrorCode.UNAUTHORIZED, "No authenticated user"));
    }
}
