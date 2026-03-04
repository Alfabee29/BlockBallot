package com.securevote.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.securevote.model.Block;
import com.securevote.model.PollOption;
import com.securevote.model.VoteRecord;
import com.securevote.model.Poll;
import com.securevote.model.VotingBlockchain;
import com.securevote.repository.PollOptionRepository;
import com.securevote.repository.PollRepository;
import com.securevote.repository.VoteRecordRepository;
import com.securevote.repository.VoterRepository;
import com.securevote.security.AuditLog;
import com.securevote.security.CryptoLayers;
import com.securevote.security.InputValidator;
import com.securevote.security.RateLimiter;

/**
 * CONCRETE IMPLEMENTATION — implements the VotingService interface.
 *
 * OOP Concepts:
 * • Interface Implementation — this class IS-A VotingService
 * • Encapsulation — all blockchain, crypto, and DB logic hidden here
 * • Single Responsibility — only handles voting business logic
 * • Dependency Injection — repositories injected via constructor
 *
 * The controller never touches the blockchain or crypto directly;
 * it only calls methods on the VotingService interface.
 */
@Service
public class VotingServiceImpl implements VotingService {

    private final PollOptionRepository optionRepo;
    private final PollRepository pollRepo;
    private final VoteRecordRepository voteRecordRepo;
    private final VoterRepository voterRepo;
    private final VotingBlockchain blockchain;

    // Rate limiters — encapsulated inside the service
    // General: 60 requests per minute, Vote casts: 20 per 5 minutes
    private final RateLimiter generalLimiter = new RateLimiter(60, 60_000);
    private final RateLimiter voteLimiter = new RateLimiter(20, 300_000);

    public VotingServiceImpl(PollOptionRepository optionRepo,
            PollRepository pollRepo,
            VoteRecordRepository voteRecordRepo,
            VoterRepository voterRepo,
            VotingBlockchain blockchain) {
        this.optionRepo = optionRepo;
        this.pollRepo = pollRepo;
        this.voteRecordRepo = voteRecordRepo;
        this.voterRepo = voterRepo;
        this.blockchain = blockchain;
    }

    @Override
    public String castVote(String voterId, int optionId, int pollId, String clientIp)
            throws VoteRejectedException {

        // 1. Rate limiting
        if (!generalLimiter.tryAcquire(clientIp) || !voteLimiter.tryAcquire(clientIp)) {
            AuditLog.log(AuditLog.EventType.VOTE_REJECTED_RATE_LIMITED,
                    "Rate limit exceeded", clientIp);
            throw new VoteRejectedException("Too many requests. Please wait before trying again.");
        }

        // 2. Validate voter ID format
        InputValidator.ValidationResult vResult = InputValidator.validateVoterId(voterId);
        if (!vResult.isValid()) {
            AuditLog.log(AuditLog.EventType.VOTE_REJECTED_INVALID_ID,
                    "Invalid voter ID format", clientIp);
            throw new VoteRejectedException(vResult.getMessage());
        }
        String normalizedId = vResult.getSanitized();

        // 3. Check voter exists in database (registered voters only)
        boolean isRegistered = voterRepo.existsById(normalizedId);
        if (!isRegistered) {
            AuditLog.log(AuditLog.EventType.VOTE_REJECTED_INVALID_ID,
                    "Unregistered voter ID", clientIp);
            throw new VoteRejectedException(
                    "This Voter ID is not registered. Please check your EPIC number.");
        }

        // 4. Validate option
        if (!InputValidator.isValidOptionId(optionId)) {
            AuditLog.log(AuditLog.EventType.VOTE_REJECTED_INVALID_OPTION,
                    "Invalid option ID: " + optionId, clientIp);
            throw new VoteRejectedException("Invalid candidate selection.");
        }

        PollOption selectedOption = optionRepo.findById(optionId).orElse(null);
        if (selectedOption == null || !Integer.valueOf(pollId).equals(selectedOption.getPollId())) {
            AuditLog.log(AuditLog.EventType.VOTE_REJECTED_INVALID_OPTION,
                    "Option not found for poll: " + optionId, clientIp);
            throw new VoteRejectedException("Invalid candidate selection.");
        }

        // 5. Derive anonymized token
        String anonymizedToken = CryptoLayers.deriveVoterToken(normalizedId, "POLL-" + pollId);

        // 6. Check for duplicate vote
        if (blockchain.hasVoted(anonymizedToken)) {
            AuditLog.log(AuditLog.EventType.VOTE_REJECTED_DUPLICATE,
                    "Duplicate vote attempt", clientIp);
            throw new VoteRejectedException(
                    "This Voter ID has already been used to cast a ballot in this election.");
        }

        // 7. Mine block and add to chain
        String prevHash = blockchain.getLatestBlockHash();
        int nextIndex = blockchain.getNextBlockIndex();
        Block newBlock = new Block(prevHash, optionId, anonymizedToken, nextIndex);
        blockchain.addBlock(newBlock);

        // 8. Persist vote record to database
        Poll poll = pollRepo.findById(pollId).orElse(null);
        if (poll != null) {
            VoteRecord record = new VoteRecord(poll, selectedOption,
                    anonymizedToken, newBlock.getCurrentHash());
            voteRecordRepo.save(record);
        }

        AuditLog.log(AuditLog.EventType.VOTE_CAST,
                "Vote recorded — block #" + nextIndex, clientIp);

        return newBlock.getCurrentHash();
    }

    @Override
    public Map<String, Integer> getTally(int pollId) {
        Map<String, Integer> tally = new HashMap<>();

        List<PollOption> candidates = optionRepo.findByPollId(pollId);
        for (PollOption c : candidates) {
            tally.put(c.getDisplayText(), 0);
        }

        for (Block block : blockchain.getChain()) {
            int votedId = block.getSelectedOptionId();
            if (votedId != 0) {
                PollOption voted = optionRepo.findById(votedId).orElse(null);
                if (voted != null) {
                    tally.merge(voted.getDisplayText(), 1, Integer::sum);
                }
            }
        }

        return tally;
    }

    @Override
    public List<PollOption> getCandidates(int pollId) {
        return optionRepo.findByPollId(pollId);
    }
}
