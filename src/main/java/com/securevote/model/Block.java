package com.securevote.model;

import java.time.Instant;
import java.util.Objects;

import com.securevote.security.CryptoLayers;

/**
 * An immutable block in the voting blockchain.
 *
 * Security model (dual-hash):
 * • powHash — fast SHA-256 hash that satisfies Proof-of-Work difficulty.
 * This makes it computationally expensive to forge a block.
 * • currentHash — strong multi-layer hash (SHA-256 → SHA3-256 → PBKDF2 →
 * HMAC-SHA512 → SHA3-256).
 * This is the authoritative block hash for chain linkage.
 * • Both hashes must be valid for the block to pass integrity checks.
 * • Timing-safe comparison prevents side-channel attacks.
 */
public class Block implements Auditable {

    private final String previousHash;
    private final int selectedOptionId;
    private final String anonymizedVoterId;
    private final long timestamp;
    private final String nonce;
    private final String powHash; // fast SHA-256 PoW hash (must satisfy difficulty)
    private final String currentHash; // strong multi-layer block hash (chain linkage)
    private final int blockIndex;

    // ── Genesis shortcut ──────────────────────────────────────
    public Block(String previousHash, int selectedOptionId, String anonymizedVoterId) {
        this(previousHash, selectedOptionId, anonymizedVoterId, 0);
    }

    public Block(String previousHash, int selectedOptionId,
            String anonymizedVoterId, int blockIndex) {
        this.previousHash = Objects.requireNonNull(previousHash, "previousHash cannot be null");
        this.selectedOptionId = selectedOptionId;
        this.anonymizedVoterId = Objects.requireNonNull(anonymizedVoterId, "anonymizedVoterId cannot be null");
        this.timestamp = Instant.now().getEpochSecond();
        this.blockIndex = blockIndex;

        // Mine a nonce that satisfies Proof-of-Work difficulty (fast SHA-256)
        String payloadPrefix = buildPayloadPrefix();
        CryptoLayers.MineResult mined = CryptoLayers.mineNonce(payloadPrefix);
        this.nonce = mined.nonce;
        this.powHash = mined.powHash;

        // Compute the authoritative block hash (strong multi-layer)
        this.currentHash = computeStrongHash();
    }

    // ── Hash computation ──────────────────────────────────────

    private String buildPayloadPrefix() {
        return previousHash + "|" + selectedOptionId + "|"
                + anonymizedVoterId + "|" + timestamp + "|" + blockIndex;
    }

    /** Strong multi-layer hash for chain integrity. */
    private String computeStrongHash() {
        String payload = buildPayloadPrefix() + "|" + nonce + "|" + powHash;
        return CryptoLayers.multiLayerHash(payload);
    }

    /** Fast SHA-256 hash for PoW verification. */
    private String recomputePowHash() {
        return CryptoLayers.fastHash(buildPayloadPrefix() + "|" + nonce);
    }

    @Override
    public String generateCryptographicHash() {
        return computeStrongHash();
    }

    /**
     * Timing-safe integrity check — verifies:
     * 1. PoW hash is correct and satisfies difficulty
     * 2. Strong block hash matches recalculation
     */
    @Override
    public boolean verifyIntegrity() {
        // Verify PoW
        String recomputedPow = recomputePowHash();
        if (!CryptoLayers.timingSafeEquals(powHash, recomputedPow))
            return false;
        if (!CryptoLayers.satisfiesDifficulty(powHash))
            return false;

        // Verify strong hash
        String recomputedStrong = generateCryptographicHash();
        return CryptoLayers.timingSafeEquals(currentHash, recomputedStrong);
    }

    // ── Getters ───────────────────────────────────────────────
    public String getCurrentHash() {
        return currentHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public int getSelectedOptionId() {
        return selectedOptionId;
    }

    public String getAnonymizedVoterId() {
        return anonymizedVoterId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNonce() {
        return nonce;
    }

    public String getPowHash() {
        return powHash;
    }

    public int getBlockIndex() {
        return blockIndex;
    }
}
