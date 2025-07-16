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
    // Senere: private final DiscordNotificationService discordService;

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
                // Her kan man legge til logikk for PUSH/VOID, men for nå antar vi tap
                bet.setStatus(PlacedBet.BetStatus.LOST);
                bet.setProfit(-bet.getStake());

                VirtualPortfolio portfolio = bet.getPortfolio();
                portfolio.setLosses(portfolio.getLosses() + 1);

                log.info("BET LOST: ID {}, Loss: {:.2f}", bet.getId(), bet.getStake());
            }
            bet.setSettledAt(Instant.now());
            // Repository lagrer endringene i portfolio automatisk pga. @Transactional

            // Her vil vi senere kalle: discordService.sendSettledBetNotification(bet);
        }
        log.info("--- [SETTLEMENT] Fullførte avgjøring av {} bets. ---", pendingBets.size());
    }

    private boolean checkWinCondition(PlacedBet bet, Fixture fixture) {
        if ("MATCH_WINNER".equalsIgnoreCase(bet.getMarket())) {
            Integer homeGoals = fixture.getGoalsHome();
            Integer awayGoals = fixture.getGoalsAway();
            if (homeGoals == null || awayGoals == null) return false; // Kan ikke avgjøre

            if ("HOME_WIN".equals(bet.getSelection()) && homeGoals > awayGoals) return true;
            if ("AWAY_WIN".equals(bet.getSelection()) && awayGoals > homeGoals) return true;
            if ("DRAW".equals(bet.getSelection()) && homeGoals.equals(awayGoals)) return true;
        }
        // TODO: Legg til logikk for andre markeder som Over/Under her
        return false;
    }
}