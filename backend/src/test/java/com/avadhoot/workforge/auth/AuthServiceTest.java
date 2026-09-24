package com.avadhoot.workforge.auth;

import com.avadhoot.workforge.auth.dto.AuthResponse;
import com.avadhoot.workforge.auth.dto.LoginRequest;
import com.avadhoot.workforge.auth.dto.MeResponse;
import com.avadhoot.workforge.auth.dto.RegisterRequest;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    private RegisterRequest newUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return new RegisterRequest("user_" + suffix, "user_" + suffix + "@wf.local",
                "Password123!", "Test User");
    }

    @Test
    void register_issuesTokensAndPersistsUser() {
        RegisterRequest request = newUser();

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.user().username()).isEqualTo(request.username());
        // Backend REPORTER role is mapped to the coarse frontend MEMBER role.
        assertThat(response.user().roles()).contains("MEMBER");
    }

    @Test
    void register_duplicateEmail_isRejected() {
        RegisterRequest first = newUser();
        authService.register(first);
        RegisterRequest duplicate = new RegisterRequest(
                "other_" + UUID.randomUUID().toString().substring(0, 6), first.email(),
                "Password123!", "Dupe");

        assertThatExceptionOfType(DuplicateResourceException.class)
                .isThrownBy(() -> authService.register(duplicate));
    }

    @Test
    void login_withValidCredentials_succeeds() {
        RegisterRequest request = newUser();
        authService.register(request);

        AuthResponse response = authService.login(new LoginRequest(request.email(), request.password()));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.user().email()).isEqualTo(request.email());
    }

    @Test
    void login_withWrongPassword_throws() {
        RegisterRequest request = newUser();
        authService.register(request);

        assertThatThrownBy(() -> authService.login(new LoginRequest(request.email(), "WrongPass1!")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refresh_rotatesToken_andInvalidatesOldOne() {
        RegisterRequest request = newUser();
        AuthResponse initial = authService.register(request);

        AuthResponse rotated = authService.refresh(initial.refreshToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(initial.refreshToken());

        assertThatThrownBy(() -> authService.refresh(initial.refreshToken()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void me_returnsUserWithPermissions() {
        RegisterRequest request = newUser();
        AuthResponse response = authService.register(request);

        MeResponse me = authService.me(Long.valueOf(response.user().id()));

        assertThat(me.user().username()).isEqualTo(request.username());
        assertThat(me.permissions()).contains("ISSUE_READ");
    }
}
