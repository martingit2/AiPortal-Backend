package com.AiPortal.repository;

import com.AiPortal.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {
    // Finner alle analyser for en spesifikk bruker, sortert etter opprettelsestidspunkt
    List<Analysis> findByUserIdOrderByCreatedAtDesc(String userId);
}