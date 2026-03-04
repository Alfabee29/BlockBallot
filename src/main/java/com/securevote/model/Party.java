package com.securevote.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the 'parties' table.
 *
 * OOP: Encapsulation — all fields are private, accessed only via getters.
 * DB: 1NF — atomic columns, no repeating groups.
 * 3NF — party_name depends only on party_id (no transitive dependencies).
 */
@Entity
@Table(name = "parties")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Integer partyId;

    @Column(name = "party_name", nullable = false, unique = true)
    private String partyName;

    @Column(name = "party_abbr", nullable = false, unique = true)
    private String partyAbbr;

    @Column(name = "founded_year")
    private Integer foundedYear;

    protected Party() {
        /* Required by JPA */ }

    // ── Getters (Encapsulation) ──────────────────────────────
    public Integer getPartyId() {
        return partyId;
    }

    public String getPartyName() {
        return partyName;
    }

    public String getPartyAbbr() {
        return partyAbbr;
    }

    public Integer getFoundedYear() {
        return foundedYear;
    }

    @Override
    public String toString() {
        return partyName + " (" + partyAbbr + ")";
    }
}
