package com.securevote.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securevote.model.Poll;

/**
 * INTERFACE — extends JpaRepository (which is itself an interface).
 *
 * OOP: Interface inheritance — PollRepository IS-A JpaRepository,
 * which provides CRUD operations automatically.
 * Spring Data JPA generates the concrete implementation at runtime
 * (Polymorphism — the framework decides the class).
 */
public interface PollRepository extends JpaRepository<Poll, Integer> {
}
