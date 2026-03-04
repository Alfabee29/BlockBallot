package com.securevote.controller;

import java.time.LocalDate;
import java.util.Random;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.securevote.model.Constituency;
import com.securevote.model.Voter;
import com.securevote.repository.ConstituencyRepository;
import com.securevote.repository.VoterRepository;
import com.securevote.security.AuditLog;
import com.securevote.security.CryptoLayers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final VoterRepository voterRepo;
    private final ConstituencyRepository constituencyRepo;

    public AuthController(VoterRepository voterRepo, ConstituencyRepository constituencyRepo) {
        this.voterRepo = voterRepo;
        this.constituencyRepo = constituencyRepo;
    }

    @GetMapping("/login")
    public String loginPage(Model model, @RequestParam(required = false) String successMsg) {
        if (successMsg != null) {
            model.addAttribute("successMsg", successMsg);
        }
        return "login";
    }

    @PostMapping("/do-login")
    public String doLogin(@RequestParam String voterId, @RequestParam String password, HttpSession session, Model model,
            HttpServletRequest request) {
        Voter voter = voterRepo.findById(voterId).orElse(null);
        String hashedInput = CryptoLayers.multiLayerHash(password);

        if (voter != null && voter.getPassword().equals(hashedInput)) {
            session.setAttribute("VOTER_ID", voter.getVoterId());
            session.setAttribute("USER_ROLE", voter.getRole());
            session.setAttribute("VOTER_NAME", voter.getVoterName());

            AuditLog.log(AuditLog.EventType.SYSTEM_STARTUP, "User logged in: " + voterId, request.getRemoteAddr());

            if ("ROLE_ADMIN".equals(voter.getRole())) {
                return "redirect:/admin";
            }
            return "redirect:/";
        }

        model.addAttribute("errorMsg", "Invalid EPIC ID or Password.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("constituencies", constituencyRepo.findAll());
        return "register";
    }

    @PostMapping("/do-register")
    public String doRegister(
            @RequestParam String voterName,
            @RequestParam int constituencyId,
            @RequestParam String dateOfBirth,
            @RequestParam String password,
            Model model,
            HttpServletRequest request) {

        Constituency c = constituencyRepo.findById(constituencyId).orElse(null);
        if (c == null) {
            model.addAttribute("errorMsg", "Invalid constituency selection.");
            model.addAttribute("constituencies", constituencyRepo.findAll());
            return "register";
        }

        // Generate unique EPIC number
        Random random = new Random();
        String epic = "EPIC" + (100000 + random.nextInt(900000));
        while (voterRepo.existsById(epic)) {
            epic = "EPIC" + (100000 + random.nextInt(900000));
        }

        Voter v = new Voter(
                epic,
                voterName,
                c,
                LocalDate.parse(dateOfBirth),
                CryptoLayers.multiLayerHash(password),
                "ROLE_VOTER");
        voterRepo.save(v);

        AuditLog.log(AuditLog.EventType.SYSTEM_STARTUP, "New elector registered: " + epic, request.getRemoteAddr());

        return "redirect:/login?successMsg=Electoral Registration Approved. Your official EPIC ID is: " + epic;
    }
}
