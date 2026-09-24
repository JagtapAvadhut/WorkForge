package com.avadhoot.workforge.notification.dto;

import com.avadhoot.workforge.notification.domain.Notification;
import com.avadhoot.workforge.notification.domain.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * Frontend-aligned notification payload. Serialises ids as strings and exposes
 * {@code body}/{@code issueKey}/{@code read} fields expected by the React client.
 */
public record NotificationResponse(
        String id,
        String type,
        String title,
        @JsonProperty("body") String body,
        String issueKey,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(Notification n) {
        String issueKey = null;
        if ("ISSUE".equalsIgnoreCase(n.getEntityType()) && n.getEntityId() != null) {
            // Entity id alone is not an issue key; keep null unless message embeds one.
            issueKey = extractIssueKey(n.getMessage());
        }
        return new NotificationResponse(
                String.valueOf(n.getId()),
                mapType(n.getType()),
                n.getTitle(),
                n.getMessage(),
                issueKey,
                n.isRead(),
                n.getCreatedAt());
    }

    private static String mapType(NotificationType type) {
        if (type == null) {
            return "STATUS_CHANGED";
        }
        return switch (type) {
            case ISSUE_ASSIGNED -> "ASSIGNED";
            case MENTIONED -> "MENTIONED";
            case ISSUE_COMMENTED -> "COMMENTED";
            case ISSUE_TRANSITIONED, ISSUE_UPDATED, WATCHING -> "STATUS_CHANGED";
        };
    }

    private static String extractIssueKey(String message) {
        if (message == null) {
            return null;
        }
        // Match patterns like MWS-12
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b([A-Z][A-Z0-9]+-\\d+)\\b")
                .matcher(message);
        return m.find() ? m.group(1) : null;
    }
}
