package com.securevote.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA Entity — maps to the 'constituencies' table.
 *
 * OOP: Encapsulation — private fields, public getters only.
 * DB: 2NF — no partial dependencies (single-column PK).
 * 3NF — state and district depend ONLY on constituency_id,
 * not on each other (no transitive dependency).
 */
@Entity
@Table(name = "constituencies")
public class Constituency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "constituency_id")
    private Integer constituencyId;

    @Column(name = "constituency_name", nullable = false)
    private String constituencyName;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "district", nullable = false)
    private String district;

    protected Constituency() {
        /* Required by JPA */ }

    // ── Getters (Encapsulation) ──────────────────────────────
    public Integer getConstituencyId() {
        return constituencyId;
    }

    public String getConstituencyName() {
        return constituencyName;
    }

    public String getState() {
        return state;
    }

    public String getDistrict() {
        return district;
    }

    /** Display format: "Constituency, State" */
    public String getFullName() {
        return constituencyName + ", " + state;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
