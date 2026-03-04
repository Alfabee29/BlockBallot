package com.securevote.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the 'vote_records' table.
 *
 * OOP: Encapsulation — immutable entity with private fields.
 * DB: Normalized — stores only FK references (poll_id, option_id),
 * not redundant candidate/party names. This is 3NF compliant.
 */
@Entity
@Table(name = "vote_records")
public class VoteRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @ManyToOne
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @ManyToOne
    @JoinColumn(name = "option_id", nullable = false)
    private PollOption pollOption;

    @Column(name = "anonymized_voter", nullable = false)
    private String anonymizedVoter;

    @Column(name = "block_hash", nullable = false)
    private String blockHash;

    @Column(name = "voted_at")
    private LocalDateTime votedAt;

    protected VoteRecord() {
        /* Required by JPA */ }

    public VoteRecord(Poll poll, PollOption option, String anonymizedVoter, String blockHash) {
        this.poll = poll;
        this.pollOption = option;
        this.anonymizedVoter = anonymizedVoter;
        this.blockHash = blockHash;
        this.votedAt = LocalDateTime.now();
    }

    // ── Getters (Encapsulation) ──────────────────────────────
    public Long getRecordId() {
        return recordId;
    }

    public Poll getPoll() {
        return poll;
    }

    public PollOption getPollOption() {
        return pollOption;
    }

    public String getAnonymizedVoter() {
        return anonymizedVoter;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public LocalDateTime getVotedAt() {
        return votedAt;
    }
}
