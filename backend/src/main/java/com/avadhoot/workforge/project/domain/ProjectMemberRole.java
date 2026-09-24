package com.avadhoot.workforge.project.domain;

import com.avadhoot.workforge.user.domain.RoleName;

/**
 * Role a user holds within a specific project. Mirrors a subset of {@link RoleName}.
 */
public enum ProjectMemberRole {
    PROJECT_ADMIN,
    PROJECT_MANAGER,
    DEVELOPER,
    REPORTER,
    VIEWER
}
