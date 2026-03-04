package com.securevote.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.securevote.model.Constituency;

public interface ConstituencyRepository extends JpaRepository<Constituency, Integer> {
}
