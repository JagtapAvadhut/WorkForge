package com.avadhoot.workforge.user.domain;

/**
 * System RBAC roles. Persisted by name in the {@code roles} table.
 */
public enum RoleName {
    SYSTEM_ADMIN,
    ORG_ADMIN,
    PROJECT_ADMIN,
    PROJECT_MANAGER,
    DEVELOPER,
    REPORTER,
    VIEWER
}
