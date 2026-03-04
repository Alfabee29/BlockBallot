package com.securevote.security;

import java.util.regex.Pattern;

/**
 * Input validation utilities.
 *
 * Strict whitelisting of voter IDs:
 * • Must be 6–20 alphanumeric characters (Indian EPIC format: 3 letters + 7
 * digits)
 * • No special characters, no SQL injection, no XSS payloads
 * • Normalized to uppercase before any processing
 *
 * Option IDs validated as positive integers within a sane range.
 */
public final class InputValidator {

    // Indian EPIC IDs: 3 uppercase letters + 7 digits (e.g. ABC1234567)
    // We also accept a looser alphanumeric pattern for flexibility.
    private static final Pattern VOTER_ID_PATTERN = Pattern.compile("^[A-Z]{2,5}[0-9]{5,15}$");

    // Loose fallback: 6–20 alphanumeric characters
    private static final Pattern VOTER_ID_LOOSE = Pattern.compile("^[A-Za-z0-9]{6,20}$");

    private static final int MAX_OPTION_ID = 999_999;

    private InputValidator() {
    }

    // ── Voter ID ─────────────────────────────────────────────

    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String sanitized;

        private ValidationResult(boolean valid, String message, String sanitized) {
            this.valid = valid;
            this.message = message;
            this.sanitized = sanitized;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        public String getSanitized() {
            return sanitized;
        }

        static ValidationResult ok(String sanitized) {
            return new ValidationResult(true, null, sanitized);
        }

        static ValidationResult fail(String message) {
            return new ValidationResult(false, message, null);
        }
    }

    /**
     * Validate and sanitize a voter ID.
     */
    public static ValidationResult validateVoterId(String voterId) {
        if (voterId == null || voterId.isBlank()) {
            return ValidationResult.fail("Voter ID is required.");
        }

        String trimmed = voterId.trim().toUpperCase();

        // Length bounds
        if (trimmed.length() < 6 || trimmed.length() > 20) {
            return ValidationResult.fail("Voter ID must be 6–20 characters.");
        }

        // Strict pattern (EPIC format)
        if (VOTER_ID_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.ok(trimmed);
        }

        // Looser alphanumeric fallback
        if (VOTER_ID_LOOSE.matcher(trimmed).matches()) {
            return ValidationResult.ok(trimmed);
        }

        return ValidationResult.fail("Voter ID must contain only letters and numbers (e.g. ABC1234567).");
    }

    // ── Option ID ────────────────────────────────────────────

    public static boolean isValidOptionId(int optionId) {
        return optionId > 0 && optionId <= MAX_OPTION_ID;
    }

    // ── Generic sanitizer (strip anything that is not alphanumeric / whitespace)
    // ──

    public static String sanitize(String input) {
        if (input == null)
            return "";
        return input.replaceAll("[^\\p{Alnum}\\p{Blank}\\-_@.]", "").trim();
    }
}
