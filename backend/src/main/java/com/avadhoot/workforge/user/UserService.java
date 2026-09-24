package com.avadhoot.workforge.user;

import com.avadhoot.workforge.common.dto.PageResponse;
import com.avadhoot.workforge.exception.DuplicateResourceException;
import com.avadhoot.workforge.exception.ResourceNotFoundException;
import com.avadhoot.workforge.user.domain.Role;
import com.avadhoot.workforge.user.domain.RoleName;
import com.avadhoot.workforge.user.domain.User;
import com.avadhoot.workforge.user.domain.UserStatus;
import com.avadhoot.workforge.user.dto.CreateUserRequest;
import com.avadhoot.workforge.user.dto.UpdateUserRequest;
import com.avadhoot.workforge.user.dto.UserResponse;
import com.avadhoot.workforge.user.repository.RoleRepository;
import com.avadhoot.workforge.user.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username already taken: " + request.username());
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName());
        user.setOrganizationId(request.organizationId());
        Set<RoleName> requested = request.roles() == null || request.roles().isEmpty()
                ? Set.of(RoleName.DEVELOPER) : request.roles();
        user.setRoles(resolveRoles(requested));
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return userMapper.toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.of(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findById(id);
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        if (request.roles() != null && !request.roles().isEmpty()) {
            user.setRoles(resolveRoles(request.roles()));
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = findById(id);
        user.setStatus(status);
        user.setEnabled(status == UserStatus.ACTIVE);
        return userMapper.toResponse(user);
    }

    /** Soft delete: disable the account rather than removing the row. */
    @Transactional
    public void softDelete(Long id) {
        User user = findById(id);
        user.setEnabled(false);
        user.setStatus(UserStatus.INACTIVE);
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = names.stream()
                .map(name -> roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("Role", name)))
                .collect(Collectors.toCollection(HashSet::new));
        return roles;
    }
}
