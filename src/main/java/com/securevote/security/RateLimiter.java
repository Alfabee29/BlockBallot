package com.securevote.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory sliding-window rate limiter per IP address.
 *
 * Prevents brute-force voter-ID enumeration and rapid-fire vote attempts.
 * Each IP is allowed {@code maxRequests} within a {@code windowMs} window.
 */
public final class RateLimiter {

    private final int maxRequests;
    private final long windowMs;

    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    /**
     * @return true if the request is allowed; false if rate-limited.
     */
    public boolean tryAcquire(String clientIp) {
        long now = System.currentTimeMillis();
        TokenBucket bucket = buckets.compute(clientIp, (key, existing) -> {
            if (existing == null || now - existing.windowStart.get() > windowMs) {
                return new TokenBucket(now);
            }
            return existing;
        });

        return bucket.counter.incrementAndGet() <= maxRequests;
    }

    /**
     * Periodically purge stale entries (called from a scheduled task or at check
     * time).
     */
    public void purgeStale() {
        long now = System.currentTimeMillis();
        buckets.entrySet().removeIf(e -> now - e.getValue().windowStart.get() > windowMs * 2);
    }

    private static class TokenBucket {
        final AtomicLong windowStart;
        final AtomicInteger counter;

        TokenBucket(long start) {
            this.windowStart = new AtomicLong(start);
            this.counter = new AtomicInteger(0);
        }
    }
}
