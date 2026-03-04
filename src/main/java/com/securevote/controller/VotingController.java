package com.securevote.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.securevote.model.VotingBlockchain;
import com.securevote.model.VotingBlockchain.ChainStats;
import com.securevote.model.VotingBlockchain.ChainVerificationResult;
import com.securevote.security.AuditLog;
import com.securevote.security.CryptoLayers;
import com.securevote.service.VotingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Web controller — handles HTTP requests.
 *
 * OOP Concepts:
 * • Dependency Inversion — depends on VotingService INTERFACE, not concrete
 * class
 * • Encapsulation — business logic is hidden in the service layer
 * • Single Responsibility — only handles HTTP request/response mapping
 */
@Controller
public class VotingController {

    private static final int POLL_ID = 101;

    // Depends on the INTERFACE (abstraction), not the concrete class
    private final VotingService votingService;
    private final VotingBlockchain blockchain;

    /**
     * Constructor injection — Spring resolves VotingService to VotingServiceImpl.
     * This is Polymorphism in action: the controller doesn't know which class
     * implements VotingService — it just calls the interface methods.
     */
    public VotingController(VotingService votingService, VotingBlockchain blockchain) {
        this.votingService = votingService;
        this.blockchain = blockchain;
        AuditLog.log(AuditLog.EventType.SYSTEM_STARTUP,
                "BlockBallot initialized — PoW difficulty: " + CryptoLayers.POW_DIFFICULTY);
    }

    // ── Helper ───────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // ── Vote page ────────────────────────────────────────────

    @GetMapping("/")
    public String showVotingPage(Model model, HttpSession session) {
        String role = (String) session.getAttribute("USER_ROLE");
        if ("ROLE_ADMIN".equals(role)) {
            return "redirect:/admin";
        }

        String voterId = (String) session.getAttribute("VOTER_ID");
        model.addAttribute("voterId", voterId);
        model.addAttribute("voterName", session.getAttribute("VOTER_NAME"));
        model.addAttribute("candidates", votingService.getCandidates(POLL_ID));
        return "vote";
    }

    // ── Cast vote — delegates to service interface ───────────

    @PostMapping("/cast-vote")
    public String castVote(@RequestParam("selectedOptionId") int optionId,
            HttpSession session,
            Model model,
            HttpServletRequest request) {

        String voterId = (String) session.getAttribute("VOTER_ID");

        try {
            String receiptHash = votingService.castVote(
                    voterId, optionId, POLL_ID, getClientIp(request));

            model.addAttribute("receiptHash", receiptHash);
            return "receipt";

        } catch (VotingService.VoteRejectedException e) {
            model.addAttribute("errorMsg", e.getUserMessage());
            model.addAttribute("candidates", votingService.getCandidates(POLL_ID));
            return "vote";

        } catch (SecurityException se) {
            AuditLog.log(AuditLog.EventType.BLOCK_INTEGRITY_FAILURE,
                    "Security exception: " + se.getMessage(), getClientIp(request));
            model.addAttribute("errorMsg",
                    "A security violation was detected. Your vote was not recorded.");
            model.addAttribute("candidates", votingService.getCandidates(POLL_ID));
            return "vote";

        } catch (Exception e) {
            AuditLog.log(AuditLog.EventType.SYSTEM_ERROR,
                    "Unhandled exception: " + e.getMessage(), getClientIp(request));
            model.addAttribute("errorMsg", "Vote could not be processed. Please retry.");
            model.addAttribute("candidates", votingService.getCandidates(POLL_ID));
            return "vote";
        }
    }

    // ── Results ──────────────────────────────────────────────

    @GetMapping("/results")
    public String showResults(Model model) {
        model.addAttribute("voteResults", votingService.getTally(POLL_ID));
        model.addAttribute("chainStats", blockchain.getChainStats());
        return "results";
    }

    // ── Ledger ───────────────────────────────────────────────

    @GetMapping("/ledger")
    public String showLedger(Model model) {
        model.addAttribute("blocks", blockchain.getChain());
        model.addAttribute("chainStats", blockchain.getChainStats());
        return "ledger";
    }

    // ── Chain verification API (JSON) ────────────────────────

    @GetMapping("/api/verify-chain")
    @ResponseBody
    public Map<String, Object> verifyChain(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        ChainVerificationResult result = blockchain.verifyFullChain();

        Map<String, Object> response = new HashMap<>();
        response.put("valid", result.isValid());
        response.put("blocksVerified", result.getBlocksVerified());
        response.put("merkleRoot", result.getMerkleRoot());
        response.put("timestamp", java.time.Instant.now().toString());

        if (!result.isValid()) {
            response.put("failedAtIndex", result.getFailedAtIndex());
            response.put("failureReason", result.getFailureReason());
            AuditLog.log(AuditLog.EventType.CHAIN_VERIFICATION_FAILED,
                    result.getFailureReason(), clientIp);
        } else {
            AuditLog.log(AuditLog.EventType.CHAIN_VERIFICATION_PASSED,
                    "All " + result.getBlocksVerified() + " blocks OK", clientIp);
        }

        return response;
    }

    // ── Chain stats API (JSON) ───────────────────────────────

    @GetMapping("/api/chain-stats")
    @ResponseBody
    public ChainStats getChainStats() {
        return blockchain.getChainStats();
    }

    // ── Audit log page ───────────────────────────────────────

    @GetMapping("/audit")
    public String showAuditLog(Model model) {
        model.addAttribute("auditEntries", AuditLog.getRecent(200));
        model.addAttribute("totalEntries", AuditLog.size());
        model.addAttribute("chainStats", blockchain.getChainStats());
        return "audit";
    }
}
