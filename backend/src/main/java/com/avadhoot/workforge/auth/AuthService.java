package com.avadhoot.workforge.auth;

import com.avadhoot.workforge.auth.domain.RefreshToken;
import com.avadhoot.workforge.auth.dto.AuthResponse;
import com.avadhoot.workforge.auth.dto.LoginRequest;
import com.avadhoot.workforge.auth.dto.MeResponse;
import com.avadhoot.workforge.auth.dto.RegisterRequest;
import com.avadhoot.workforge.auth.repository.RefreshTokenRepository;
import com.avadhoot.workforge.common.ErrorCode;
import com.avadhoot.workforge.config.props.JwtProperties;
import com.avadhoot.workforge.exception.BusinessException;
import com.avadhoot.workforge.exception.DuplicateResourceException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.security.JwtService;
import com.avadhoot.workforge.security.UserPrincipal;
import com.avadhoot.workforge.user.UserMapper;
import com.avadhoot.workforge.user.domain.Role;
import com.avadhoot.workforge.user.domain.RoleName;
import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.repository.RoleRepository;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       JwtProperties jwtProperties, UserMapper userMapper) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken");
        }
        Role defaultRole = roleRepository.findByName(RoleName.REPORTER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role REPORTER not configured"));
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);
        user.setRoles(roles);
        user = userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.login(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Invalid refresh token"));
        if (!stored.isActive()) {
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token expired or revoked");
        }
        // Rotation: revoke the presented token and mint a fresh pair.
        stored.setRevoked(true);
        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", stored.getUserId()));
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Set<String> permissions = UserPrincipal.from(user).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toSet());
        return new MeResponse(userMapper.toResponse(user), permissions);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername());
        String rawRefresh = UUID.randomUUID() + "-" + new java.math.BigInteger(160, secureRandom).toString(36);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(jwtService.hashToken(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMillis()));
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.of(accessToken, rawRefresh,
                jwtProperties.getAccessExpiration() / 1000, userMapper.toResponse(user));
    }
}
