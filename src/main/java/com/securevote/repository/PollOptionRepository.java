package com.securevote.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securevote.model.PollOption;

/**
 * INTERFACE — extends JpaRepository for PollOption entities.
 *
 * OOP Concepts:
 * • Interface Inheritance — this extends JpaRepository which extends
 * PagingAndSortingRepository which extends CrudRepository.
 * This is a multi-level interface hierarchy.
 * • Abstraction — the SQL query for findByPollId is auto-generated
 * by Spring Data JPA from the method name. We define WHAT we want,
 * not HOW to get it.
 * • Polymorphism — at runtime, Spring creates a proxy class that
 * implements this interface. The actual class is decided at runtime.
 */
public interface PollOptionRepository extends JpaRepository<PollOption, Integer> {

    /**
     * Derived query — Spring generates:
     * SELECT * FROM poll_options WHERE poll_id = ?
     */
    List<PollOption> findByPollId(Integer pollId);
}