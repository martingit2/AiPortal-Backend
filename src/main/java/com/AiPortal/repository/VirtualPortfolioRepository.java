// src/main/java/com/AiPortal/repository/VirtualPortfolioRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.VirtualPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VirtualPortfolioRepository extends JpaRepository<VirtualPortfolio, Long> {

    // --- OPPDATERT METODE ---
    // Brukes av BettingSimulationRunner for å unngå lazy-loading feil.
    @Query("SELECT p FROM VirtualPortfolio p JOIN FETCH p.model WHERE p.isActive = true")
    List<VirtualPortfolio> findActivePortfoliosWithModel();

    @Query("SELECT p FROM VirtualPortfolio p JOIN FETCH p.model")
    List<VirtualPortfolio> findAllWithModel();
}