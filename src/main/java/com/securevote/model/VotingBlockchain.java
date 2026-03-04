package com.securevote.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.securevote.security.CryptoLayers;

/**
 * Thread-safe in-memory blockchain for the voting ledger.
 *
 * Hardened features:
 * • Full-chain integrity validation (every block re-verified)
 * • Previous-hash linkage check
 * • Proof-of-Work verification on each block
 * • Merkle root computation for O(log N) tamper evidence
 * • Chain statistics (block count, vote count, etc.)
 * • Immutable chain view returned to callers
 */
public class VotingBlockchain {

    private final List<Block> chain = new ArrayList<>();

    public VotingBlockchain() {
        // Genesis block — index 0
        chain.add(new Block("GENESIS", 0, "GENESIS", 0));
    }

    // ── Core operations ──────────────────────────────────────

    public synchronized void addBlock(Block newBlock) {
        Objects.requireNonNull(newBlock, "newBlock must not be null");

        // 1. Verify block self-integrity (hash + PoW)
        if (!newBlock.verifyIntegrity()) {
            throw new SecurityException("Block integrity check failed — possible tampering.");
        }

        // 2. Verify chain linkage
        String latestHash = getLatestBlockHash();
        if (!CryptoLayers.timingSafeEquals(latestHash, newBlock.getPreviousHash())) {
            throw new SecurityException("Block previous hash does not match chain tip — chain fork rejected.");
        }

        // 3. Verify block index is sequential
        if (newBlock.getBlockIndex() != chain.size()) {
            throw new SecurityException("Block index mismatch — expected " + chain.size()
                    + " but got " + newBlock.getBlockIndex());
        }

        chain.add(newBlock);
    }

    public synchronized String getLatestBlockHash() {
        return chain.get(chain.size() - 1).getCurrentHash();
    }

    public synchronized int getNextBlockIndex() {
        return chain.size();
    }

    public synchronized boolean hasVoted(String anonymizedVoterId) {
        Objects.requireNonNull(anonymizedVoterId, "anonymizedVoterId must not be null");
        for (Block block : chain) {
            if (CryptoLayers.timingSafeEquals(anonymizedVoterId, block.getAnonymizedVoterId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a defensive copy of the chain — callers cannot mutate the ledger.
     */
    public synchronized List<Block> getChain() {
        return Collections.unmodifiableList(new ArrayList<>(chain));
    }

    // ── Full-chain integrity verification ────────────────────

    /**
     * Result record for chain verification.
     */
    public static class ChainVerificationResult {
        private final boolean valid;
        private final int blocksVerified;
        private final int failedAtIndex; // -1 if all passed
        private final String failureReason;
        private final String merkleRoot;

        public ChainVerificationResult(boolean valid, int blocksVerified,
                int failedAtIndex, String failureReason,
                String merkleRoot) {
            this.valid = valid;
            this.blocksVerified = blocksVerified;
            this.failedAtIndex = failedAtIndex;
            this.failureReason = failureReason;
            this.merkleRoot = merkleRoot;
        }

        public boolean isValid() {
            return valid;
        }

        public int getBlocksVerified() {
            return blocksVerified;
        }

        public int getFailedAtIndex() {
            return failedAtIndex;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public String getMerkleRoot() {
            return merkleRoot;
        }
    }

    /**
     * Walk the entire chain and verify:
     * 1. Each block's self-integrity (hash recalculation + PoW)
     * 2. Each block's previousHash links to the prior block's currentHash
     * 3. Block indices are sequential
     * Also computes the Merkle root.
     */
    public synchronized ChainVerificationResult verifyFullChain() {
        List<String> allHashes = new ArrayList<>();

        for (int i = 0; i < chain.size(); i++) {
            Block block = chain.get(i);

            // Self-integrity
            if (!block.verifyIntegrity()) {
                return new ChainVerificationResult(false, i, i,
                        "Block " + i + ": hash integrity check failed", null);
            }

            // Block index
            if (block.getBlockIndex() != i) {
                return new ChainVerificationResult(false, i, i,
                        "Block " + i + ": index mismatch (expected " + i
                                + ", got " + block.getBlockIndex() + ")",
                        null);
            }

            // Chain linkage (skip genesis)
            if (i > 0) {
                String expectedPrev = chain.get(i - 1).getCurrentHash();
                if (!CryptoLayers.timingSafeEquals(expectedPrev, block.getPreviousHash())) {
                    return new ChainVerificationResult(false, i, i,
                            "Block " + i + ": previousHash does not match block "
                                    + (i - 1) + "'s currentHash",
                            null);
                }
            }

            allHashes.add(block.getCurrentHash());
        }

        String merkleRoot = CryptoLayers.computeMerkleRoot(allHashes);
        return new ChainVerificationResult(true, chain.size(), -1, null, merkleRoot);
    }

    // ── Chain statistics ─────────────────────────────────────

    public static class ChainStats {
        private final int totalBlocks;
        private final int totalVotes;
        private final String merkleRoot;
        private final long genesisTimestamp;
        private final long latestTimestamp;
        private final String latestHash;

        public ChainStats(int totalBlocks, int totalVotes, String merkleRoot,
                long genesisTimestamp, long latestTimestamp, String latestHash) {
            this.totalBlocks = totalBlocks;
            this.totalVotes = totalVotes;
            this.merkleRoot = merkleRoot;
            this.genesisTimestamp = genesisTimestamp;
            this.latestTimestamp = latestTimestamp;
            this.latestHash = latestHash;
        }

        public int getTotalBlocks() {
            return totalBlocks;
        }

        public int getTotalVotes() {
            return totalVotes;
        }

        public String getMerkleRoot() {
            return merkleRoot;
        }

        public long getGenesisTimestamp() {
            return genesisTimestamp;
        }

        public long getLatestTimestamp() {
            return latestTimestamp;
        }

        public String getLatestHash() {
            return latestHash;
        }
    }

    public synchronized ChainStats getChainStats() {
        int votes = 0;
        for (Block b : chain) {
            if (b.getSelectedOptionId() != 0)
                votes++;
        }

        List<String> hashes = chain.stream()
                .map(Block::getCurrentHash)
                .collect(Collectors.toList());
        String merkle = CryptoLayers.computeMerkleRoot(hashes);

        return new ChainStats(
                chain.size(),
                votes,
                merkle,
                chain.get(0).getTimestamp(),
                chain.get(chain.size() - 1).getTimestamp(),
                chain.get(chain.size() - 1).getCurrentHash());
    }
}
