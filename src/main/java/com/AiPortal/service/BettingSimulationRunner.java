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
    @Scheduled(fixedRate = 60000, initialDelay = 10000) // Kjører hvert minutt for testing
    public void findAndPlaceBets() {
        log.info("--- [BETTING SIM] Starter jobb for å finne og plassere bets ---");

        List<VirtualPortfolio> activePortfolios = portfolioRepository.findActivePortfoliosWithModel();

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

            for (Fixture fixture : fixturesToAnalyze) {
                boolean alreadyBet = placedBetRepository.findByPortfolioIdOrderByPlacedAtDesc(portfolio.getId())
                        .stream().anyMatch(b -> b.getFixtureId().equals(fixture.getId()));

                if (alreadyBet) {
                    continue;
                }

                // Tilbakestilt til å kalle den enklere metoden
                List<ValueBetDto> valueBets = oddsCalculationService.calculateValue(fixture);

                for (ValueBetDto valueBet : valueBets) {
                    processSingleValueBet(valueBet, portfolio);
                }
            }
        }
        log.info("--- [BETTING SIM] Fullførte jobb. ---");
    }

    private void processSingleValueBet(ValueBetDto valueBet, VirtualPortfolio portfolio) {
        double bestValue = 0.0;
        String bestSelection = null;
        double bestOdds = 0.0;
        double modelProb = 0.0;
        String market = "";

        String marketDesc = valueBet.getMarketDescription() != null ? valueBet.getMarketDescription() : "";

        if (marketDesc.contains("Over/Under")) {
            if (valueBet.getValueHome() > bestValue) {
                bestValue = valueBet.getValueHome(); bestSelection = "OVER"; bestOdds = valueBet.getMarketHomeOdds();
                modelProb = isFinite(valueBet.getAracanixHomeOdds()) ? 1 / valueBet.getAracanixHomeOdds() : 0; market = "Over/Under 2.5";
            }
            if (valueBet.getValueAway() > bestValue) {
                bestValue = valueBet.getValueAway(); bestSelection = "UNDER"; bestOdds = valueBet.getMarketAwayOdds();
                modelProb = isFinite(valueBet.getAracanixAwayOdds()) ? 1 / valueBet.getAracanixAwayOdds() : 0; market = "Over/Under 2.5";
            }
        }
        else if (marketDesc.contains("Kampvinner")) {
            if (valueBet.getValueHome() > bestValue) {
                bestValue = valueBet.getValueHome(); bestSelection = "HOME_WIN"; bestOdds = valueBet.getMarketHomeOdds();
                modelProb = isFinite(valueBet.getAracanixHomeOdds()) ? 1 / valueBet.getAracanixHomeOdds() : 0; market = "Kampvinner";
            }
            if (valueBet.getValueDraw() > bestValue) {
                bestValue = valueBet.getValueDraw(); bestSelection = "DRAW"; bestOdds = valueBet.getMarketDrawOdds();
                modelProb = isFinite(valueBet.getAracanixDrawOdds()) ? 1 / valueBet.getAracanixDrawOdds() : 0; market = "Kampvinner";
            }
            if (valueBet.getValueAway() > bestValue) {
                bestValue = valueBet.getValueAway(); bestSelection = "AWAY_WIN"; bestOdds = valueBet.getMarketAwayOdds();
                modelProb = isFinite(valueBet.getAracanixAwayOdds()) ? 1 / valueBet.getAracanixAwayOdds() : 0; market = "Kampvinner";
            }
        }

        if (bestSelection != null && bestValue > 0.0) { // Bruker 0.0 som terskel for testing
            double stake = portfolio.getCurrentBalance() * 0.01;
            if (stake < 1.0) return;

            PlacedBet newBet = new PlacedBet();
            newBet.setPortfolio(portfolio);
            newBet.setFixtureId(valueBet.getFixtureId());
            newBet.setMarket(market);
            newBet.setSelection(bestSelection);
            newBet.setStake(stake);
            newBet.setOdds(bestOdds);
            newBet.setModelProbability(modelProb);
            newBet.setValue(bestValue);
            newBet.setStatus(PlacedBet.BetStatus.PENDING);

            placedBetRepository.save(newBet);

            portfolio.setCurrentBalance(portfolio.getCurrentBalance() - stake);
            portfolio.setTotalBets(portfolio.getTotalBets() + 1);

            log.info("PLACED BET: {} på {} for kamp {}. Stake: {:.2f}, Odds: {:.2f}, Value: {:.2f}%",
                    bestSelection, market, valueBet.getFixtureId(), stake, bestOdds, bestValue * 100);
        }
    }

    private boolean isFinite(double value) {
        return Double.isFinite(value);
    }
}