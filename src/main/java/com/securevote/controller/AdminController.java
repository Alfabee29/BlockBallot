package com.securevote.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.securevote.repository.VoterRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {

    private final VoterRepository voterRepo;

    public AdminController(VoterRepository voterRepo) {
        this.voterRepo = voterRepo;
    }

    @GetMapping("/admin")
    public String adminPage(Model model, HttpSession session) {
        String role = (String) session.getAttribute("USER_ROLE");
        if (!"ROLE_ADMIN".equals(role)) {
            return "redirect:/";
        }

        model.addAttribute("voters", voterRepo.findAll());
        return "admin";
    }
}
