// src/main/java/com/AiPortal/repository/PlacedBetRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PlacedBet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacedBetRepository extends JpaRepository<PlacedBet, Long> {

    // Finner alle bets som venter på å bli avgjort for kamper som er ferdige
    @Query("SELECT pb FROM PlacedBet pb JOIN Fixture f ON pb.fixtureId = f.id WHERE pb.status = 'PENDING' AND f.status IN ('FT', 'AET', 'PEN')")
    List<PlacedBet> findPendingBetsForFinishedFixtures();

    // Finner alle bets for en spesifikk portefølje, sortert etter dato
    List<PlacedBet> findByPortfolioIdOrderByPlacedAtDesc(Long portfolioId);
}