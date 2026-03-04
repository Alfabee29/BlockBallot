package com.securevote.security;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Immutable audit log for all security-relevant events.
 *
 * All entries are timestamped and stored in a thread-safe deque.
 * The log is append-only — entries cannot be modified or deleted.
 */
public final class AuditLog {

    private static final int MAX_ENTRIES = 10_000;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());

    private static final ConcurrentLinkedDeque<AuditEntry> LOG = new ConcurrentLinkedDeque<>();

    private AuditLog() {
    }

    // ── Event types ──────────────────────────────────────────

    public enum EventType {
        VOTE_CAST,
        VOTE_REJECTED_DUPLICATE,
        VOTE_REJECTED_INVALID_ID,
        VOTE_REJECTED_INVALID_OPTION,
        VOTE_REJECTED_RATE_LIMITED,
        CHAIN_VERIFICATION_PASSED,
        CHAIN_VERIFICATION_FAILED,
        BLOCK_INTEGRITY_FAILURE,
        SYSTEM_STARTUP,
        SYSTEM_ERROR
    }

    // ── Entry record ─────────────────────────────────────────

    public static class AuditEntry {
        private final long timestamp;
        private final String formattedTime;
        private final EventType eventType;
        private final String detail;
        private final String clientIp;

        public AuditEntry(EventType eventType, String detail, String clientIp) {
            this.timestamp = Instant.now().toEpochMilli();
            this.formattedTime = FORMATTER.format(Instant.now());
            this.eventType = eventType;
            this.detail = detail;
            this.clientIp = clientIp != null ? maskIp(clientIp) : "SYSTEM";
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getFormattedTime() {
            return formattedTime;
        }

        public EventType getEventType() {
            return eventType;
        }

        public String getDetail() {
            return detail;
        }

        public String getClientIp() {
            return clientIp;
        }

        /** Mask last octet of IPv4 for privacy. */
        private static String maskIp(String ip) {
            if (ip == null)
                return "UNKNOWN";
            int lastDot = ip.lastIndexOf('.');
            if (lastDot > 0)
                return ip.substring(0, lastDot) + ".***";
            return ip.length() > 4 ? ip.substring(0, 4) + "***" : ip;
        }
    }

    // ── Logging ──────────────────────────────────────────────

    public static void log(EventType type, String detail, String clientIp) {
        LOG.addFirst(new AuditEntry(type, detail, clientIp));
        // Trim old entries
        while (LOG.size() > MAX_ENTRIES) {
            LOG.removeLast();
        }
    }

    public static void log(EventType type, String detail) {
        log(type, detail, null);
    }

    /** Return the most recent N entries (newest first). */
    public static List<AuditEntry> getRecent(int count) {
        List<AuditEntry> result = new ArrayList<>();
        int i = 0;
        for (AuditEntry entry : LOG) {
            if (i++ >= count)
                break;
            result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }

    /** Return all entries (newest first). */
    public static List<AuditEntry> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(LOG));
    }

    public static int size() {
        return LOG.size();
    }
}
