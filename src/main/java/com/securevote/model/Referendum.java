package com.securevote.model;

/**
 * CONCRETE CLASS — a referendum (yes/no vote on a policy question).
 *
 * OOP Concepts:
 * • Inheritance — extends AbstractPoll
 * • Polymorphism — overrides getPollType() to return "REFERENDUM"
 * • Encapsulation — adds its own private field (question)
 *
 * Demonstrates that the SAME abstract base class can produce
 * different behaviors via different subclasses (polymorphism).
 */
public class Referendum extends AbstractPoll {

    private final String question;

    public Referendum(int pollId, String title, String question) {
        super(pollId, title);
        this.question = question != null ? question : title;
    }

    @Override
    public String getPollType() {
        return "REFERENDUM";
    }

    @Override
    public int getMaxVotesPerVoter() {
        return 1;
    }

    public String getQuestion() {
        return question;
    }

    @Override
    public String getDisplayTitle() {
        return super.getDisplayTitle() + " — Q: " + question;
    }
}
