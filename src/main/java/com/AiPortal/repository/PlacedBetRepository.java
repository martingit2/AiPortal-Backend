// src/main/java/com/AiPortal/repository/PlacedBetRepository.java
package com.AiPortal.repository;

import com.AiPortal.entity.PlacedBet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlacedBetRepository extends JpaRepository<PlacedBet, Long> {

    /**
     * Finner alle ventende bets hvor den tilknyttede kampen nå er ferdigspilt.
     * Bruker JOIN FETCH for å hente portefølje og modell i samme spørring for å unngå N+1-problemer.
     *
     * @return En liste med bets som er klare for å bli avgjort.
     */
    @Query("SELECT pb FROM PlacedBet pb JOIN FETCH pb.portfolio p JOIN FETCH p.model WHERE pb.status = 'PENDING' AND pb.fixtureId IN (SELECT f.id FROM Fixture f WHERE f.status IN ('FT', 'AET', 'PEN'))")
    List<PlacedBet> findPendingBetsForFinishedFixtures();

    /**
     * Henter alle bets for en gitt portefølje, sortert med de nyeste først.
     *
     * @param portfolioId ID-en til porteføljen.
     * @return En liste med bets.
     */
    List<PlacedBet> findByPortfolioIdOrderByPlacedAtDesc(Long portfolioId);

    /**
     * NY, OPTIMALISERT METODE:
     * Teller antall rader som matcher en gitt portefølje-ID og status.
     * Dette er mye raskere enn å hente hele listen med objekter bare for å telle dem.
     *
     * @param portfolioId ID-en til porteføljen.
     * @param status Statusen på spillene som skal telles (f.eks. PENDING).
     * @return Antallet spill som matcher kriteriene.
     */
    long countByPortfolioIdAndStatus(Long portfolioId, PlacedBet.BetStatus status);
}