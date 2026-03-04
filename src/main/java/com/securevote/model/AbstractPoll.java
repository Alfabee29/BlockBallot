package com.securevote.model;

/**
 * ABSTRACT CLASS — base class for all poll types.
 *
 * OOP Concepts demonstrated:
 * • Abstraction — cannot be instantiated directly, provides a template
 * • Encapsulation — protected fields accessed only via getters
 * • Inheritance — concrete poll types extend this class
 * • Polymorphism — getPollType() is abstract, each subclass provides its own
 *
 * This class defines WHAT a poll is (common attributes) but NOT
 * how specific poll types behave — that is left to subclasses.
 */
public abstract class AbstractPoll {

    // ── Encapsulated fields (protected for subclass access) ──
    protected final int pollId;
    protected final String title;

    protected AbstractPoll(int pollId, String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Poll title cannot be null or blank");
        }
        this.pollId = pollId;
        this.title = title;
    }

    // ── Getters (Encapsulation — controlled access) ──────────
    public int getPollId() {
        return pollId;
    }

    public String getTitle() {
        return title;
    }

    /**
     * ABSTRACT METHOD — subclasses MUST override this.
     * This is the key polymorphism point: calling getPollType()
     * on an AbstractPoll reference dispatches to the correct subclass.
     *
     * @return the type of poll (e.g. "GENERAL_ELECTION", "REFERENDUM")
     */
    public abstract String getPollType();

    /**
     * ABSTRACT METHOD — each poll type has its own voting rules.
     * 
     * @return maximum number of votes one voter can cast
     */
    public abstract int getMaxVotesPerVoter();

    /**
     * Template method (concrete) — shared by all poll types.
     * 
     * @return formatted display string
     */
    public String getDisplayTitle() {
        return "[" + getPollType() + "] " + title;
    }

    @Override
    public String toString() {
        return getDisplayTitle() + " (ID: " + pollId + ")";
    }
}