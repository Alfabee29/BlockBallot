package com.securevote.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the 'polls' table.
 *
 * OOP: Inheritance — this is also a GeneralElection at the domain level.
 * Encapsulation — private fields, getters only.
 */
@Entity
@Table(name = "polls")
public class Poll {

    @Id
    @Column(name = "poll_id")
    private Integer pollId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "poll_type", nullable = false)
    private String pollType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive;

    protected Poll() {
        /* Required by JPA */ }

    // ── Getters ──────────────────────────────────────────────
    public Integer getPollId() {
        return pollId;
    }

    public String getTitle() {
        return title;
    }

    public String getPollType() {
        return pollType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Business logic — is the poll currently open?
     */
    public boolean isOpen() {
        if (!Boolean.TRUE.equals(isActive))
            return false;
        LocalDate today = LocalDate.now();
        if (startDate != null && today.isBefore(startDate))
            return false;
        if (endDate != null && today.isAfter(endDate))
            return false;
        return true;
    }
}
