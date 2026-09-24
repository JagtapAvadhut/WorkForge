package com.avadhoot.workforge.user.domain;

import java.util.Set;

/**
 * Fine-grained permissions granted to roles. Persisted by name in {@code permissions}.
 */
public enum PermissionName {
    USER_MANAGE,
    USER_READ,
    ORG_MANAGE,
    PROJECT_CREATE,
    PROJECT_MANAGE,
    PROJECT_READ,
    ISSUE_CREATE,
    ISSUE_READ,
    ISSUE_UPDATE,
    ISSUE_DELETE,
    ISSUE_TRANSITION,
    ISSUE_ASSIGN,
    COMMENT_CREATE,
    COMMENT_MANAGE,
    SPRINT_MANAGE,
    BOARD_MANAGE,
    WORKFLOW_MANAGE,
    FILTER_MANAGE,
    DASHBOARD_MANAGE;

    /**
     * Default permission grants per role, used by the DataSeeder.
     */
    public static Set<PermissionName> forRole(RoleName role) {
        return switch (role) {
            case SYSTEM_ADMIN, ORG_ADMIN -> Set.of(values());
            case PROJECT_ADMIN -> Set.of(
                    PROJECT_MANAGE, PROJECT_READ, PROJECT_CREATE, USER_READ,
                    ISSUE_CREATE, ISSUE_READ, ISSUE_UPDATE, ISSUE_DELETE, ISSUE_TRANSITION, ISSUE_ASSIGN,
                    COMMENT_CREATE, COMMENT_MANAGE, SPRINT_MANAGE, BOARD_MANAGE, WORKFLOW_MANAGE,
                    FILTER_MANAGE, DASHBOARD_MANAGE);
            case PROJECT_MANAGER -> Set.of(
                    PROJECT_READ, USER_READ,
                    ISSUE_CREATE, ISSUE_READ, ISSUE_UPDATE, ISSUE_TRANSITION, ISSUE_ASSIGN,
                    COMMENT_CREATE, COMMENT_MANAGE, SPRINT_MANAGE, BOARD_MANAGE,
                    FILTER_MANAGE, DASHBOARD_MANAGE);
            case DEVELOPER -> Set.of(
                    PROJECT_READ, USER_READ,
                    ISSUE_CREATE, ISSUE_READ, ISSUE_UPDATE, ISSUE_TRANSITION, ISSUE_ASSIGN,
                    COMMENT_CREATE, FILTER_MANAGE, DASHBOARD_MANAGE);
            case REPORTER -> Set.of(
                    PROJECT_READ, USER_READ,
                    ISSUE_CREATE, ISSUE_READ, COMMENT_CREATE, FILTER_MANAGE, DASHBOARD_MANAGE);
            case VIEWER -> Set.of(PROJECT_READ, ISSUE_READ, USER_READ);
        };
    }
}
