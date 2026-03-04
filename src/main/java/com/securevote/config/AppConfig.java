package com.securevote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.securevote.model.VotingBlockchain;

/**
 * Application configuration — creates shared beans.
 *
 * The VotingBlockchain is a singleton — only one chain exists
 * for the entire application lifecycle.
 */
@Configuration
public class AppConfig {

    @Bean
    public VotingBlockchain votingBlockchain() {
        return new VotingBlockchain();
    }

    @Bean
    public org.springframework.boot.ApplicationRunner initPasswords(com.securevote.repository.VoterRepository repo) {
        return args -> {
            // Seed passwords if default
            for (com.securevote.model.Voter v : repo.findAll()) {
                if ("PENDING".equals(v.getPassword())) {
                    String cleanId = v.getVoterId().toLowerCase();
                    // Just default to 'password123' for standard
                    // or for ADMIN use something specific
                    String pwd = "ADMIN".equalsIgnoreCase(cleanId) ? "admin123" : "password123";

                    var newVoter = new com.securevote.model.Voter(
                            v.getVoterId(), v.getVoterName(), v.getConstituency(),
                            v.getDateOfBirth(), com.securevote.security.CryptoLayers.multiLayerHash(pwd), v.getRole());
                    repo.save(newVoter);
                }
            }
        };
    }
}
