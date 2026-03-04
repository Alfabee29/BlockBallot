-- ═══════════════════════════════════════════════════════
-- BlockBallot Database Schema
-- Normalized to BCNF (Boyce-Codd Normal Form)
-- ═══════════════════════════════════════════════════════

-- Drop in correct dependency order (child → parent)
DROP TABLE IF EXISTS audit_events;
DROP TABLE IF EXISTS vote_records;
DROP TABLE IF EXISTS poll_options;
DROP TABLE IF EXISTS polls;
DROP TABLE IF EXISTS constituencies;
DROP TABLE IF EXISTS parties;
DROP TABLE IF EXISTS voters;

-- ═══════════════════════════════════════════════════════
-- 1NF: Every column is atomic, every row unique (PK)
-- ═══════════════════════════════════════════════════════

-- Parties table — no repeating groups, atomic columns
CREATE TABLE parties (
    party_id    INT PRIMARY KEY AUTO_INCREMENT,
    party_name  VARCHAR(100)  NOT NULL UNIQUE,
    party_abbr  VARCHAR(10)   NOT NULL UNIQUE,
    founded_year INT
);

-- Constituencies — atomic location data, no multi-valued attributes
CREATE TABLE constituencies (
    constituency_id   INT PRIMARY KEY AUTO_INCREMENT,
    constituency_name VARCHAR(150) NOT NULL,
    state             VARCHAR(100) NOT NULL,
    district          VARCHAR(100) NOT NULL,
    UNIQUE (constituency_name, state)
);

-- ═══════════════════════════════════════════════════════
-- 2NF: No partial dependencies — every non-key column
--       depends on the WHOLE primary key
-- ═══════════════════════════════════════════════════════

-- Voters — voter_id is the sole PK; all attributes depend on it entirely
CREATE TABLE voters (
    voter_id          VARCHAR(20)  PRIMARY KEY,
    voter_name        VARCHAR(150) NOT NULL,
    constituency_id   INT          NOT NULL,
    date_of_birth     DATE         NOT NULL,
    registered_on     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    is_active         BOOLEAN      DEFAULT TRUE,
    password          VARCHAR(255) NOT NULL,
    role              VARCHAR(50)  NOT NULL DEFAULT 'ROLE_VOTER',
    FOREIGN KEY (constituency_id) REFERENCES constituencies(constituency_id)
);

-- Polls — independent entity; title depends only on poll_id
CREATE TABLE polls (
    poll_id       INT PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    poll_type     VARCHAR(30)  NOT NULL DEFAULT 'GENERAL_ELECTION',
    start_date    DATE,
    end_date      DATE,
    is_active     BOOLEAN      DEFAULT TRUE
);

-- ═══════════════════════════════════════════════════════
-- 3NF / BCNF: No transitive dependencies
-- Candidate's party is a FK → party details live in parties table
-- Candidate's constituency is a FK → constituency details live separately
-- ═══════════════════════════════════════════════════════

-- Poll options (candidates) — party and constituency are foreign keys,
-- not stored as text (eliminating transitive dependency)
CREATE TABLE poll_options (
    option_id         INT PRIMARY KEY AUTO_INCREMENT,
    poll_id           INT          NOT NULL,
    option_text       VARCHAR(255) NOT NULL,
    party_id          INT,
    constituency_id   INT,
    FOREIGN KEY (poll_id)          REFERENCES polls(poll_id),
    FOREIGN KEY (party_id)         REFERENCES parties(party_id),
    FOREIGN KEY (constituency_id)  REFERENCES constituencies(constituency_id)
);

-- Vote records — each vote is stored with FK references only (no redundant data)
CREATE TABLE vote_records (
    record_id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    poll_id           INT          NOT NULL,
    option_id         INT          NOT NULL,
    anonymized_voter  VARCHAR(200) NOT NULL,
    block_hash        VARCHAR(200) NOT NULL,
    voted_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (poll_id)   REFERENCES polls(poll_id),
    FOREIGN KEY (option_id) REFERENCES poll_options(option_id)
);

-- ═══════════════════════════════════════════════════════
-- Audit events table — persisted security log
-- ═══════════════════════════════════════════════════════
CREATE TABLE audit_events (
    event_id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type  VARCHAR(50)  NOT NULL,
    detail      VARCHAR(500),
    client_ip   VARCHAR(50),
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);