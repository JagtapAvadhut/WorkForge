package com.avadhoot.workforge.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        @JsonProperty("login")
        @JsonAlias({"usernameOrEmail", "username", "email"})
        String login,
        @NotBlank String password
) {
}
