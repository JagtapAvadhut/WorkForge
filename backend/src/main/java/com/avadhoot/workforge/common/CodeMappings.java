package com.avadhoot.workforge.common;

import com.avadhoot.workforge.project.domain.ProjectMemberRole;
import com.avadhoot.workforge.sprint.domain.SprintState;
import com.avadhoot.workforge.user.domain.RoleName;

/**
 * Central translation between backend reference-data names / enums and the
 * compact codes used by the React frontend contract.
 */
public final class CodeMappings {

    private CodeMappings() {
    }

    // ---- Issue type: entity name <-> frontend code ----

    public static String issueTypeCode(String name) {
        if (name == null) {
            return null;
        }
        return switch (name.trim().toLowerCase()) {
            case "task" -> "TASK";
            case "bug" -> "BUG";
            case "story" -> "STORY";
            case "epic" -> "EPIC";
            case "sub-task", "subtask", "sub_task" -> "SUBTASK";
            default -> name.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        };
    }

    public static String issueTypeName(String code) {
        if (code == null) {
            return null;
        }
        return switch (code.trim().toUpperCase()) {
            case "TASK" -> "Task";
            case "BUG" -> "Bug";
            case "STORY" -> "Story";
            case "EPIC" -> "Epic";
            case "SUBTASK", "SUB_TASK", "SUB-TASK" -> "Sub-task";
            default -> code.trim();
        };
    }

    // ---- Priority: entity name <-> frontend code ----

    public static String priorityCode(String name) {
        return name == null ? null : name.trim().toUpperCase();
    }

    public static String priorityName(String code) {
        if (code == null) {
            return null;
        }
        String c = code.trim();
        if (c.isEmpty()) {
            return c;
        }
        return c.substring(0, 1).toUpperCase() + c.substring(1).toLowerCase();
    }

    // ---- Sprint state: entity <-> frontend code (CLOSED == COMPLETED) ----

    public static String sprintStateCode(SprintState state) {
        if (state == null) {
            return null;
        }
        return state == SprintState.CLOSED ? "COMPLETED" : state.name();
    }

    // ---- RBAC role: backend RoleName -> coarse frontend Role ----

    public static String frontendRole(RoleName name) {
        if (name == null) {
            return "VIEWER";
        }
        return switch (name) {
            case SYSTEM_ADMIN, ORG_ADMIN -> "ADMIN";
            case PROJECT_ADMIN, PROJECT_MANAGER -> "PROJECT_LEAD";
            case DEVELOPER, REPORTER -> "MEMBER";
            case VIEWER -> "VIEWER";
        };
    }

    // ---- Project membership role: entity <-> frontend ProjectRole ----

    public static String projectRoleCode(ProjectMemberRole role) {
        if (role == null) {
            return "MEMBER";
        }
        return switch (role) {
            case PROJECT_ADMIN, PROJECT_MANAGER -> "LEAD";
            case DEVELOPER, REPORTER -> "MEMBER";
            case VIEWER -> "VIEWER";
        };
    }

    public static ProjectMemberRole projectMemberRole(String frontendRole) {
        if (frontendRole == null) {
            return ProjectMemberRole.DEVELOPER;
        }
        return switch (frontendRole.trim().toUpperCase()) {
            case "LEAD" -> ProjectMemberRole.PROJECT_ADMIN;
            case "VIEWER" -> ProjectMemberRole.VIEWER;
            case "MEMBER" -> ProjectMemberRole.DEVELOPER;
            // Also accept raw backend enum names for flexibility.
            case "PROJECT_ADMIN" -> ProjectMemberRole.PROJECT_ADMIN;
            case "PROJECT_MANAGER" -> ProjectMemberRole.PROJECT_MANAGER;
            case "DEVELOPER" -> ProjectMemberRole.DEVELOPER;
            case "REPORTER" -> ProjectMemberRole.REPORTER;
            default -> ProjectMemberRole.DEVELOPER;
        };
    }

    /** Parse a string id (as sent by the frontend) into a Long, tolerating blanks. */
    public static Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
