package com.avadhoot.workforge.user.dto;

import com.avadhoot.workforge.user.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateUserRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Size(max = 150) String fullName,
        Long organizationId,
        Set<RoleName> roles
) {
}
