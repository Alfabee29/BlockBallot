package com.securevote.service;

import java.util.List;
import java.util.Map;

/**
 * SERVICE INTERFACE — defines the contract for voting operations.
 *
 * OOP Concept: Abstraction via Interface
 * • The controller depends on this INTERFACE, not on the concrete class.
 * • This allows swapping implementations (e.g., for testing) without
 * changing the controller code — Dependency Inversion Principle.
 * • The HOW of each operation is hidden; only the WHAT is specified.
 */
public interface VotingService {

    /**
     * Cast a vote for a candidate.
     * 
     * @return the receipt hash of the mined block
     * @throws VoteRejectedException if the vote is invalid
     */
    String castVote(String voterId, int optionId, int pollId, String clientIp)
            throws VoteRejectedException;

    /**
     * Get vote tally for a poll.
     * 
     * @return map of candidate name → vote count
     */
    Map<String, Integer> getTally(int pollId);

    /**
     * Get all candidates for a poll.
     */
    List<? extends Object> getCandidates(int pollId);

    /**
     * Custom exception for vote rejections — Encapsulation of error details.
     */
    class VoteRejectedException extends Exception {
        private final String userMessage;

        public VoteRejectedException(String userMessage) {
            super(userMessage);
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }
}
