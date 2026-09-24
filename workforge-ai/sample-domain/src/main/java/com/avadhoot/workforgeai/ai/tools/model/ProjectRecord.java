package com.avadhoot.workforgeai.ai.tools.model;

public record ProjectRecord(
        String key,
        String name,
        String description,
        String lead,
        long issueCount
) {
}
