// src/main/java/com/AiPortal/repository/PlacedBetRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PlacedBet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacedBetRepository extends JpaRepository<PlacedBet, Long> {

    // --- OPPDATERT SPØRRING ---
    // Henter bets, tilhørende portefølje, OG porteføljens modell i én smell.
    @Query("SELECT pb FROM PlacedBet pb JOIN FETCH pb.portfolio p JOIN FETCH p.model WHERE pb.status = 'PENDING' AND pb.fixtureId IN (SELECT f.id FROM Fixture f WHERE f.status IN ('FT', 'AET', 'PEN'))")
    List<PlacedBet> findPendingBetsForFinishedFixtures();

    List<PlacedBet> findByPortfolioIdOrderByPlacedAtDesc(Long portfolioId);
}