// src/main/java/com/AiPortal/service/BettingSimulationRunner.java
package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.PlacedBet;
import com.AiPortal.entity.VirtualPortfolio;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.MatchOddsRepository;
import com.AiPortal.repository.PlacedBetRepository;
import com.AiPortal.repository.VirtualPortfolioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BettingSimulationRunner {

    private static final Logger log = LoggerFactory.getLogger(BettingSimulationRunner.class);

    private final VirtualPortfolioRepository portfolioRepository;
    private final PlacedBetRepository placedBetRepository;
    private final OddsCalculationService oddsCalculationService;
    private final MatchOddsRepository matchOddsRepository;
    private final FixtureRepository fixtureRepository;
    // Senere: private final DiscordNotificationService discordService;

    public BettingSimulationRunner(VirtualPortfolioRepository portfolioRepository,
                                   PlacedBetRepository placedBetRepository,
                                   OddsCalculationService oddsCalculationService,
                                   MatchOddsRepository matchOddsRepository,
                                   FixtureRepository fixtureRepository) {
        this.portfolioRepository = portfolioRepository;
        this.placedBetRepository = placedBetRepository;
        this.oddsCalculationService = oddsCalculationService;
        this.matchOddsRepository = matchOddsRepository;
        this.fixtureRepository = fixtureRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Kjører hver time
    public void findAndPlaceBets() {
        log.info("--- [BETTING SIM] Starter jobb for å finne og plassere bets ---");
        List<VirtualPortfolio> activePortfolios = portfolioRepository.findByIsActiveTrue();

        if (activePortfolios.isEmpty()) {
            log.info("--- [BETTING SIM] Ingen aktive porteføljer funnet. Avslutter jobb. ---");
            return;
        }

        List<Long> fixtureIdsWithOdds = matchOddsRepository.findDistinctFixtureIdsWithUpcomingOdds(Instant.now());
        if (fixtureIdsWithOdds.isEmpty()) {
            log.info("--- [BETTING SIM] Ingen kommende kamper med odds funnet. Avslutter jobb. ---");
            return;
        }

        List<Fixture> fixturesToAnalyze = fixtureRepository.findAllById(fixtureIdsWithOdds);
        fixturesToAnalyze.sort(Comparator.comparing(Fixture::getDate));

        for (VirtualPortfolio portfolio : activePortfolios) {
            log.info("--- [BETTING SIM] Analyserer for portefølje: {}", portfolio.getName());

            // Vi simulerer her at hver portefølje har sin egen analyse.
            // I en mer avansert versjon ville OddsCalculationService tatt inn en modell-ID.
            // For nå bruker den standard-modellene.

            for (Fixture fixture : fixturesToAnalyze) {
                List<ValueBetDto> valueBets = oddsCalculationService.calculateValue(fixture);

                for (ValueBetDto valueBet : valueBets) {
                    processSingleValueBet(valueBet, portfolio);
                }
            }
        }
        log.info("--- [BETTING SIM] Fullførte jobb. ---");
    }

    private void processSingleValueBet(ValueBetDto valueBet, VirtualPortfolio portfolio) {
        // Enkel logikk: finn det beste utfallet (høyest verdi)
        double bestValue = 0;
        String bestSelection = null;
        double bestOdds = 0;
        double modelProb = 0;

        if (valueBet.getValueHome() > bestValue) {
            bestValue = valueBet.getValueHome();
            bestSelection = "HOME_WIN";
            bestOdds = valueBet.getMarketHomeOdds();
            modelProb = 1 / valueBet.getAracanixHomeOdds();
        }
        if (valueBet.getValueDraw() > bestValue) {
            bestValue = valueBet.getValueDraw();
            bestSelection = "DRAW";
            bestOdds = valueBet.getMarketDrawOdds();
            modelProb = 1 / valueBet.getAracanixDrawOdds();
        }
        if (valueBet.getValueAway() > bestValue) {
            bestValue = valueBet.getValueAway();
            bestSelection = "AWAY_WIN";
            bestOdds = valueBet.getMarketAwayOdds();
            modelProb = 1 / valueBet.getAracanixAwayOdds();
        }

        // Plasser bet hvis verdien er over en viss terskel (f.eks. 10%)
        if (bestSelection != null && bestValue > 0.10) {
            // Enkel staking-strategi: Alltid satse 1% av nåværende saldo
            double stake = portfolio.getCurrentBalance() * 0.01;

            PlacedBet newBet = new PlacedBet();
            newBet.setPortfolio(portfolio);
            newBet.setFixtureId(valueBet.getFixtureId());
            newBet.setMarket(valueBet.getMarketDescription());
            newBet.setSelection(bestSelection);
            newBet.setStake(stake);
            newBet.setOdds(bestOdds);
            newBet.setModelProbability(modelProb);
            newBet.setValue(bestValue);
            newBet.setStatus(PlacedBet.BetStatus.PENDING);

            placedBetRepository.save(newBet);

            // Oppdater porteføljen (dette blir commitet samlet på slutten av transaksjonen)
            portfolio.setCurrentBalance(portfolio.getCurrentBalance() - stake);
            portfolio.setTotalBets(portfolio.getTotalBets() + 1);

            log.info("PLACED BET: {} på {} for kamp {}. Stake: {:.2f}, Odds: {:.2f}, Value: {:.2f}%",
                    bestSelection, valueBet.getMarketDescription(), valueBet.getFixtureId(), stake, bestOdds, bestValue * 100);

            // Her vil vi senere kalle: discordService.sendNewBetNotification(newBet);
        }
    }
}