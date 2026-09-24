package com.avadhoot.workforge.security;

/**
 * Minimal thread-safe token bucket (greedy refill) used by {@link RateLimitFilter}.
 * Kept dependency-free and in-memory by design.
 */
public class TokenBucket {

    private final long capacity;
    private final double refillPerNano;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, long refillTokens, long refillPeriodSeconds) {
        this.capacity = capacity;
        this.tokens = capacity;
        long periodNanos = Math.max(1L, refillPeriodSeconds) * 1_000_000_000L;
        this.refillPerNano = (double) refillTokens / periodNanos;
        this.lastRefillNanos = System.nanoTime();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        double replenished = (now - lastRefillNanos) * refillPerNano;
        if (replenished > 0) {
            tokens = Math.min(capacity, tokens + replenished);
            lastRefillNanos = now;
        }
    }
}
