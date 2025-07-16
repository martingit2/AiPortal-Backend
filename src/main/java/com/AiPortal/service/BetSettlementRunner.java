// src/main/java/com/AiPortal/service/BetSettlementRunner.java
package com.AiPortal.service;

import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.PlacedBet;
import com.AiPortal.entity.VirtualPortfolio;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.PlacedBetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class BetSettlementRunner {

    private static final Logger log = LoggerFactory.getLogger(BetSettlementRunner.class);

    private final PlacedBetRepository placedBetRepository;
    private final FixtureRepository fixtureRepository;

    public BetSettlementRunner(PlacedBetRepository placedBetRepository, FixtureRepository fixtureRepository) {
        this.placedBetRepository = placedBetRepository;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 900000) // Kjører hvert 15. minutt
    public void settleBets() {
        log.info("--- [SETTLEMENT] Starter jobb for å avgjøre bets ---");
        List<PlacedBet> pendingBets = placedBetRepository.findPendingBetsForFinishedFixtures();

        if (pendingBets.isEmpty()) {
            log.info("--- [SETTLEMENT] Ingen bets å avgjøre. ---");
            return;
        }

        for (PlacedBet bet : pendingBets) {
            Optional<Fixture> fixtureOpt = fixtureRepository.findById(bet.getFixtureId());
            if (fixtureOpt.isEmpty()) {
                log.warn("Kunne ikke finne fixture med ID {} for å avgjøre bet. Hopper over.", bet.getFixtureId());
                continue;
            }

            Fixture fixture = fixtureOpt.get();
            boolean won = checkWinCondition(bet, fixture);

            if (won) {
                bet.setStatus(PlacedBet.BetStatus.WON);
                double profit = bet.getStake() * (bet.getOdds() - 1);
                bet.setProfit(profit);

                VirtualPortfolio portfolio = bet.getPortfolio();
                portfolio.setCurrentBalance(portfolio.getCurrentBalance() + bet.getStake() + profit);
                portfolio.setWins(portfolio.getWins() + 1);

                log.info("BET WON: ID {}, Profit: {:.2f}", bet.getId(), profit);
            } else {
                bet.setStatus(PlacedBet.BetStatus.LOST);
                bet.setProfit(-bet.getStake());

                VirtualPortfolio portfolio = bet.getPortfolio();
                portfolio.setLosses(portfolio.getLosses() + 1);

                log.info("BET LOST: ID {}, Loss: {:.2f}", bet.getId(), bet.getStake());
            }
            bet.setSettledAt(Instant.now());
        }
        log.info("--- [SETTLEMENT] Fullførte avgjøring av {} bets. ---", pendingBets.size());
    }

    // --- OPPDATERT METODE ---
    private boolean checkWinCondition(PlacedBet bet, Fixture fixture) {
        Integer homeGoals = fixture.getGoalsHome();
        Integer awayGoals = fixture.getGoalsAway();
        if (homeGoals == null || awayGoals == null) return false; // Kan ikke avgjøre uten resultat

        String market = bet.getMarket() != null ? bet.getMarket() : "";
        String selection = bet.getSelection();

        if (market.contains("Kampvinner")) {
            if ("HOME_WIN".equals(selection) && homeGoals > awayGoals) return true;
            if ("AWAY_WIN".equals(selection) && awayGoals > homeGoals) return true;
            if ("DRAW".equals(selection) && homeGoals.equals(awayGoals)) return true;
        }
        else if (market.contains("Over/Under")) {
            // Antar at markedet er O/U 2.5 for nå
            int totalGoals = homeGoals + awayGoals;
            if ("OVER".equalsIgnoreCase(selection) && totalGoals > 2.5) return true;
            if ("UNDER".equalsIgnoreCase(selection) && totalGoals < 2.5) return true;
            // Legg til logikk for PUSH (nøyaktig 2.5) hvis du ønsker det
        }

        return false;
    }
}