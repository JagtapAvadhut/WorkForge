package com.avadhoot.workforgeai.ai.security;

public record SecurityCheckResult(
        SecurityStatus status,
        String reason,
        boolean allowed
) {
    public static SecurityCheckResult safe() {
        return new SecurityCheckResult(SecurityStatus.SAFE, "Input looks normal", true);
    }

    public static SecurityCheckResult suspicious(String reason) {
        return new SecurityCheckResult(SecurityStatus.SUSPICIOUS, reason, false);
    }

    public static SecurityCheckResult blocked(String reason) {
        return new SecurityCheckResult(SecurityStatus.BLOCKED, reason, false);
    }
}
