package com.securevote.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the 'voters' table.
 *
 * OOP: Encapsulation — private fields, public getters, no setters
 * (immutable after JPA hydrates the object).
 *
 * DB: 2NF — every non-key field depends on the whole PK (voter_id).
 * 3NF — constituency details are NOT stored here; only the FK
 * (constituency_id) is stored, eliminating transitive
 * dependency: voter_id → constituency_id → state/district.
 */
@Entity
@Table(name = "voters")
public class Voter {

    @Id
    @Column(name = "voter_id")
    private String voterId;

    @Column(name = "voter_name", nullable = false)
    private String voterName;

    @ManyToOne
    @JoinColumn(name = "constituency_id", nullable = false)
    private Constituency constituency;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "registered_on")
    private LocalDateTime registeredOn;

    @Column(name = "is_active")
    private Boolean isActive;

    protected Voter() {
        /* Required by JPA */ }

    // ── Getters (Encapsulation — no setters exposed) ─────────
    public String getVoterId() {
        return voterId;
    }

    public String getVoterName() {
        return voterName;
    }

    public Constituency getConstituency() {
        return constituency;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public LocalDateTime getRegisteredOn() {
        return registeredOn;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Business logic method — checks if voter is eligible.
     * Encapsulation: the eligibility rules are hidden inside the class.
     */
    public boolean isEligible() {
        if (!Boolean.TRUE.equals(isActive))
            return false;
        if (dateOfBirth == null)
            return false;
        return dateOfBirth.plusYears(18).isBefore(LocalDate.now())
                || dateOfBirth.plusYears(18).isEqual(LocalDate.now());
    }

    @Override
    public String toString() {
        return voterName + " (" + voterId + ")";
    }
}
