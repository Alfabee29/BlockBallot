package com.securevote.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the 'poll_options' table (candidates).
 *
 * OOP: Encapsulation — all fields private, no setters.
 *
 * DB: 3NF / BCNF — party and constituency are FK references,
 * NOT stored as text. This eliminates transitive dependencies:
 * ✗ WRONG: option_id → party_name → party_abbr (transitive)
 * ✓ RIGHT: option_id → party_id (FK to parties table)
 */
@Entity
@Table(name = "poll_options")
public class PollOption {

    @Id
    @Column(name = "option_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer optionId;

    @Column(name = "poll_id", nullable = false)
    private Integer pollId;

    @Column(name = "option_text", nullable = false)
    private String optionText;

    @ManyToOne
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne
    @JoinColumn(name = "constituency_id")
    private Constituency constituency;

    protected PollOption() {
        /* Required by JPA */ }

    // ── Getters (Encapsulation — no setters exposed) ─────────
    public Integer getOptionId() {
        return optionId;
    }

    public Integer getPollId() {
        return pollId;
    }

    public String getOptionText() {
        return optionText;
    }

    public Party getParty() {
        return party;
    }

    public Constituency getConstituency() {
        return constituency;
    }

    /**
     * Display format including party abbreviation.
     * Encapsulation: formatting logic lives inside the entity.
     */
    public String getDisplayText() {
        if (party != null) {
            return optionText + " (" + party.getPartyAbbr() + ")";
        }
        return optionText;
    }
}
