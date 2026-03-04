package com.securevote.model;

/**
 * CONCRETE CLASS — a general election poll.
 *
 * OOP Concepts:
 * • Inheritance — extends AbstractPoll (inherits pollId, title,
 * getDisplayTitle)
 * • Polymorphism — overrides getPollType() and getMaxVotesPerVoter()
 * • Encapsulation — adds its own private field (constituencyScope)
 *
 * A GeneralElection allows exactly 1 vote per voter.
 */
public class GeneralElection extends AbstractPoll {

    private final String constituencyScope;

    public GeneralElection(int pollId, String title, String constituencyScope) {
        super(pollId, title); // Call parent constructor
        this.constituencyScope = constituencyScope != null ? constituencyScope : "NATIONAL";
    }

    @Override
    public String getPollType() {
        return "GENERAL_ELECTION";
    }

    @Override
    public int getMaxVotesPerVoter() {
        return 1; // Exactly one vote per voter in a general election
    }

    // ── Encapsulated getter ──────────────────────────────────
    public String getConstituencyScope() {
        return constituencyScope;
    }

    @Override
    public String getDisplayTitle() {
        return super.getDisplayTitle() + " — Scope: " + constituencyScope;
    }
}
