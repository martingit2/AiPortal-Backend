// src/main/java/com/AiPortal/repository/VirtualPortfolioRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.VirtualPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <-- NY IMPORT
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VirtualPortfolioRepository extends JpaRepository<VirtualPortfolio, Long> {

    // Finner alle aktive porteføljer for betting-jobben
    List<VirtualPortfolio> findByIsActiveTrue();

    // --- NY METODE ---
    // Henter alle porteføljer og "eagerly" laster den tilknyttede modellen
    // for å unngå LazyInitializationException ved serialisering.
    @Query("SELECT p FROM VirtualPortfolio p JOIN FETCH p.model")
    List<VirtualPortfolio> findAllWithModel();
}