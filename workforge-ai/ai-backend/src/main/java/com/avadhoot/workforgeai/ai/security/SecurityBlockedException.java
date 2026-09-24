package com.avadhoot.workforgeai.ai.security;

public class SecurityBlockedException extends RuntimeException {

    private final SecurityStatus status;
    private final String reason;

    public SecurityBlockedException(SecurityStatus status, String reason) {
        super(reason);
        this.status = status == null ? SecurityStatus.BLOCKED : status;
        this.reason = reason == null ? "Blocked by security policy" : reason;
    }

    public SecurityStatus status() {
        return status;
    }

    public String reason() {
        return reason;
    }
}
