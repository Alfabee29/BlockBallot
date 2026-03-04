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
}
