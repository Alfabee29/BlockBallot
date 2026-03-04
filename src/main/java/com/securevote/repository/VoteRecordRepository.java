package com.securevote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securevote.model.VoteRecord;

/**
 * INTERFACE — Spring Data JPA repository for VoteRecord entities.
 *
 * OOP: Interface — query contract with auto-generated implementation.
 * Custom query method (findByPollPollId) is derived from method naming
 * convention — Spring generates the SQL automatically.
 */
public interface VoteRecordRepository extends JpaRepository<VoteRecord, Long> {

    List<VoteRecord> findByPollPollId(Integer pollId);
}
