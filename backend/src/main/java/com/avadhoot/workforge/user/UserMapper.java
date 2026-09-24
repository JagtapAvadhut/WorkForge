package com.avadhoot.workforge.user;

import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.dto.UserResponse;
import org.springframework.stereotype.Component;

/**
 * Maps {@link User} entities to the frontend-aligned {@link UserResponse}.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return UserResponse.from(user);
    }
}
