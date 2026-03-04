-- ═══════════════════════════════════════════════════════
-- Seed Data for BlockBallot
-- ═══════════════════════════════════════════════════════

-- Parties
INSERT INTO parties (party_name, party_abbr, founded_year) VALUES ('Bharatiya Janata Party', 'BJP', 1980);
INSERT INTO parties (party_name, party_abbr, founded_year) VALUES ('Indian National Congress', 'INC', 1885);
INSERT INTO parties (party_name, party_abbr, founded_year) VALUES ('Aam Aadmi Party', 'AAP', 2012);
INSERT INTO parties (party_name, party_abbr, founded_year) VALUES ('Independent', 'IND', NULL);

-- Constituencies
INSERT INTO constituencies (constituency_name, state, district) VALUES ('New Delhi', 'Delhi', 'Central Delhi');
INSERT INTO constituencies (constituency_name, state, district) VALUES ('Mumbai North', 'Maharashtra', 'Mumbai Suburban');
INSERT INTO constituencies (constituency_name, state, district) VALUES ('Varanasi', 'Uttar Pradesh', 'Varanasi');

-- Poll
INSERT INTO polls (poll_id, title, poll_type, start_date, end_date, is_active)
VALUES (101, 'Lok Sabha General Election 2026', 'GENERAL_ELECTION', '2026-03-01', '2026-03-31', TRUE);

-- Candidates (poll_options)
INSERT INTO poll_options (poll_id, option_text, party_id, constituency_id) VALUES (101, 'Alice Smith', 1, 1);
INSERT INTO poll_options (poll_id, option_text, party_id, constituency_id) VALUES (101, 'Bob Johnson', 2, 1);
INSERT INTO poll_options (poll_id, option_text, party_id, constituency_id) VALUES (101, 'Charlie Kumar', 3, 1);
INSERT INTO poll_options (poll_id, option_text, party_id, constituency_id) VALUES (101, 'Diana Patel', 4, 1);

-- Registered voters
INSERT INTO voters (voter_id, voter_name, constituency_id, date_of_birth)
VALUES ('ABC1234567', 'Rahul Sharma', 1, '1990-05-15');
INSERT INTO voters (voter_id, voter_name, constituency_id, date_of_birth)
VALUES ('DEF7654321', 'Priya Gupta', 1, '1985-11-22');
INSERT INTO voters (voter_id, voter_name, constituency_id, date_of_birth)
VALUES ('GHI9876543', 'Amit Verma', 1, '1998-01-30');
INSERT INTO voters (voter_id, voter_name, constituency_id, date_of_birth)
VALUES ('JKL4567890', 'Sunita Devi', 1, '1975-08-10');