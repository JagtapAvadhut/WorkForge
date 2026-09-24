package com.avadhoot.workforgeai.ai.tools.model;

public record IssueRecord(
        String issueKey,
        String summary,
        String status,
        String priority,
        String assignee,
        String project,
        String issueType,
        String sprint
) {
}
