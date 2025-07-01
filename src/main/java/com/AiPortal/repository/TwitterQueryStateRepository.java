package com.AiPortal.repository;

import com.AiPortal.entity.TwitterQueryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TwitterQueryStateRepository extends JpaRepository<TwitterQueryState, String> {
    // Standard JpaRepository-metoder er tilstrekkelig
}