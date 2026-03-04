package com.securevote.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Hardened cryptographic utility belt.
 *
 * Layers:
 * 1. SHA-256 — fast first-pass hash
 * 2. SHA3-256 — second algorithm family (diversity)
 * 3. PBKDF2-HMAC-SHA256 — key-stretching to defeat GPU/ASIC brute-force
 * 4. HMAC-SHA512 — keyed MAC with server pepper (upgraded from SHA256)
 * 5. (block hash only) final SHA3-256 compression
 *
 * Additions in this hardened version:
 * • PBKDF2 iterations raised to 310 000 (OWASP 2024 recommendation)
 * • HMAC upgraded to SHA-512
 * • Nonce size increased to 32 bytes
 * • Timing-safe comparison for hash verification
 * • Proof-of-Work support with configurable difficulty
 * • Merkle root computation for chain-wide tamper evidence
 */
public final class CryptoLayers {

    // ── Tuning constants ─────────────────────────────────────────
    private static final int PBKDF2_ITERATIONS = 310_000;
    private static final int PBKDF2_KEY_LENGTH = 256;
    private static final int NONCE_BYTES = 32;
    public static final int POW_DIFFICULTY = 3; // leading hex zeros required

    private static final String SERVER_PEPPER = System.getProperty(
            "securevote.pepper",
            "x9!kL#mQ$2vB&pZ8wR@uJ7eF*dN4cA0"); // strong default; override via -D flag

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoLayers() {
    }

    // ── Public API ───────────────────────────────────────────────

    /**
     * Derive an election-scoped, irreversible voter token.
     * Even with the same voter ID, different election scopes produce different
     * tokens.
     */
    public static String deriveVoterToken(String voterId, String electionScope) {
        try {
            String normalized = voterId == null ? "" : voterId.trim().toUpperCase();

            String layer1 = sha256Hex(normalized);
            String layer2 = sha3_256Hex(layer1 + ":" + electionScope);
            byte[] layer3 = pbkdf2(layer2.toCharArray(),
                    electionScope.getBytes(StandardCharsets.UTF_8));
            return hmac512Hex(SERVER_PEPPER, bytesToHex(layer3));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to derive voter token", ex);
        }
    }

    /**
     * Multi-layer hash for block content.
     * SHA-256 → SHA3-256 → PBKDF2 → HMAC-SHA512 → SHA3-256
     */
    public static String multiLayerHash(String input) {
        try {
            String layer1 = sha256Hex(input);
            String layer2 = sha3_256Hex(layer1);
            byte[] layer3 = pbkdf2(layer2.toCharArray(),
                    SERVER_PEPPER.getBytes(StandardCharsets.UTF_8));
            String layer4 = hmac512Hex(SERVER_PEPPER, bytesToHex(layer3));
            return sha3_256Hex(layer4);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to compute layered hash", ex);
        }
    }

    /**
     * Cryptographically-secure 32-byte nonce encoded as URL-safe Base64.
     */
    public static String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // ── Proof-of-Work ────────────────────────────────────────────

    /**
     * Check if a hash satisfies the Proof-of-Work difficulty
     * (must start with N leading hex zeros).
     */
    public static boolean satisfiesDifficulty(String hash) {
        if (hash == null || hash.length() < POW_DIFFICULTY)
            return false;
        for (int i = 0; i < POW_DIFFICULTY; i++) {
            if (hash.charAt(i) != '0')
                return false;
        }
        return true;
    }

    /**
     * Fast SHA-256 hash for Proof-of-Work mining.
     * PoW uses plain SHA-256 (microseconds per attempt) to keep mining
     * practical, while the final block hash still uses the full
     * multi-layer pipeline for maximum security.
     */
    public static String fastHash(String input) {
        return sha256HexSafe(input);
    }

    /** Result of a PoW mining operation. */
    public static class MineResult {
        public final String nonce;
        public final String powHash;

        public MineResult(String nonce, String powHash) {
            this.nonce = nonce;
            this.powHash = powHash;
        }
    }

    /**
     * Mine a valid nonce for the given payload prefix.
     * Uses fast SHA-256 for mining speed, NOT the full multi-layer hash.
     * Returns both the nonce and the PoW hash that satisfied difficulty.
     */
    public static MineResult mineNonce(String payloadPrefix) {
        while (true) {
            String candidateNonce = generateNonce();
            String powHash = sha256HexSafe(payloadPrefix + "|" + candidateNonce);
            if (satisfiesDifficulty(powHash)) {
                return new MineResult(candidateNonce, powHash);
            }
        }
    }

    // ── Merkle Tree ──────────────────────────────────────────────

    /**
     * Compute the Merkle root of a list of block hashes.
     * Provides O(log N) tamper evidence across the entire chain.
     */
    public static String computeMerkleRoot(List<String> hashes) {
        if (hashes == null || hashes.isEmpty())
            return sha256HexSafe("");
        if (hashes.size() == 1)
            return hashes.get(0);

        java.util.List<String> layer = new java.util.ArrayList<>(hashes);
        while (layer.size() > 1) {
            java.util.List<String> nextLayer = new java.util.ArrayList<>();
            for (int i = 0; i < layer.size(); i += 2) {
                String left = layer.get(i);
                String right = (i + 1 < layer.size()) ? layer.get(i + 1) : left; // duplicate last if odd
                nextLayer.add(sha256HexSafe(left + right));
            }
            layer = nextLayer;
        }
        return layer.get(0);
    }

    // ── Timing-safe comparison ───────────────────────────────────

    /**
     * Constant-time comparison to prevent timing side-channel attacks.
     */
    public static boolean timingSafeEquals(String a, String b) {
        if (a == null || b == null)
            return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    // ── Private primitives ───────────────────────────────────────

    private static byte[] pbkdf2(char[] value, byte[] salt) throws GeneralSecurityException {
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(value, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH);
        return skf.generateSecret(spec).getEncoded();
    }

    /** HMAC-SHA512 (upgraded from SHA-256). */
    private static String hmac512Hex(String key, String value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        mac.init(secretKey);
        return bytesToHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static String sha256Hex(String input) throws GeneralSecurityException {
        return digestHex("SHA-256", input);
    }

    private static String sha256HexSafe(String input) {
        try {
            return sha256Hex(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String sha3_256Hex(String input) throws GeneralSecurityException {
        return digestHex("SHA3-256", input);
    }

    private static String digestHex(String algorithm, String input) throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        return bytesToHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
