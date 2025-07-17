// src/main/java/com/AiPortal/service/BettingSimulationRunner.java
package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.AnalysisModel;
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
    @Scheduled(fixedRate = 300000, initialDelay = 30000) // Kjører hvert 5. minutt
    public void findAndPlaceBets() {
        log.info("--- [BETTING SIM V2] Starter jobb for å finne og plassere bets ---");

        List<VirtualPortfolio> activePortfolios = portfolioRepository.findActivePortfoliosWithModel();

        if (activePortfolios.isEmpty()) {
            log.info("--- [BETTING SIM V2] Ingen aktive porteføljer funnet. Avslutter jobb. ---");
            return;
        }

        List<Long> fixtureIdsWithOdds = matchOddsRepository.findDistinctFixtureIdsWithUpcomingOdds(Instant.now());
        if (fixtureIdsWithOdds.isEmpty()) {
            log.info("--- [BETTING SIM V2] Ingen kommende kamper med odds funnet. Avslutter jobb. ---");
            return;
        }

        List<Fixture> fixturesToAnalyze = fixtureRepository.findAllById(fixtureIdsWithOdds);
        fixturesToAnalyze.sort(Comparator.comparing(Fixture::getDate));

        for (VirtualPortfolio portfolio : activePortfolios) {
            AnalysisModel model = portfolio.getModel(); // Modellen er EAGER lastet, ingen ekstra DB-kall
            if (model == null) {
                log.warn("Portefølje {} (ID: {}) mangler en tilknyttet modell. Hopper over.", portfolio.getName(), portfolio.getId());
                continue;
            }

            log.info("--- [BETTING SIM V2] Analyserer for portefølje '{}' med modell '{}' ({}) ---",
                    portfolio.getName(), model.getModelName(), model.getMarketType());

            for (Fixture fixture : fixturesToAnalyze) {
                // Sjekk om det allerede er plassert et bet for denne kampen for denne porteføljen
                boolean alreadyBet = placedBetRepository.findByPortfolioIdOrderByPlacedAtDesc(portfolio.getId())
                        .stream().anyMatch(b -> b.getFixtureId().equals(fixture.getId()));

                if (alreadyBet) {
                    continue; // Hopp over hvis bet allerede er plassert
                }

                // *** DEN KRITISKE ENDRINGEN ER HER ***
                // Vi sender nå med modell-informasjon til kalkulasjonstjenesten
                List<ValueBetDto> valueBets = oddsCalculationService.calculateValue(
                        fixture,
                        model.getModelName(),      // Send med modellens filnavn
                        model.getMarketType()      // Send med modellens markedstype
                );

                // Siden kallet nå er spesifikt, forventer vi kun én DTO (eller ingen)
                if (!valueBets.isEmpty()) {
                    processSingleValueBet(valueBets.get(0), portfolio);
                    // Hvis vi bare vil ha ett bet per kjøring, kan vi 'break' her for å gå til neste portefølje
                }
            }
        }
        log.info("--- [BETTING SIM V2] Fullførte jobb. ---");
    }

    private void processSingleValueBet(ValueBetDto valueBet, VirtualPortfolio portfolio) {
        double bestValue = 0.0;
        String bestSelection = null;
        double bestOdds = 0.0;
        double modelProb = 0.0;
        String market = "";

        String marketDesc = valueBet.getMarketDescription() != null ? valueBet.getMarketDescription() : "";

        if (marketDesc.contains("Over/Under")) {
            market = "Over/Under 2.5";
            // valueHome representerer Over, valueAway representerer Under
            if (valueBet.getValueHome() > bestValue) {
                bestValue = valueBet.getValueHome(); bestSelection = "OVER"; bestOdds = valueBet.getMarketHomeOdds();
                modelProb = isFinite(valueBet.getAracanixHomeOdds()) ? 1 / valueBet.getAracanixHomeOdds() : 0;
            }
            if (valueBet.getValueAway() > bestValue) {
                bestValue = valueBet.getValueAway(); bestSelection = "UNDER"; bestOdds = valueBet.getMarketAwayOdds();
                modelProb = isFinite(valueBet.getAracanixAwayOdds()) ? 1 / valueBet.getAracanixAwayOdds() : 0;
            }
        }
        else if (marketDesc.contains("Kampvinner")) {
            market = "Kampvinner";
            if (valueBet.getValueHome() > bestValue) {
                bestValue = valueBet.getValueHome(); bestSelection = "HOME_WIN"; bestOdds = valueBet.getMarketHomeOdds();
                modelProb = isFinite(valueBet.getAracanixHomeOdds()) ? 1 / valueBet.getAracanixHomeOdds() : 0;
            }
            if (valueBet.getValueDraw() > bestValue) {
                bestValue = valueBet.getValueDraw(); bestSelection = "DRAW"; bestOdds = valueBet.getMarketDrawOdds();
                modelProb = isFinite(valueBet.getAracanixDrawOdds()) ? 1 / valueBet.getAracanixDrawOdds() : 0;
            }
            if (valueBet.getValueAway() > bestValue) {
                bestValue = valueBet.getValueAway(); bestSelection = "AWAY_WIN"; bestOdds = valueBet.getMarketAwayOdds();
                modelProb = isFinite(valueBet.getAracanixAwayOdds()) ? 1 / valueBet.getAracanixAwayOdds() : 0;
            }
        }

        if (bestSelection != null && bestValue > 0.05) { // Bruker 5% value som en fornuftig terskel
            double stake = portfolio.getCurrentBalance() * 0.01; // Kelly-lignende: 1% av bankroll
            if (stake < 1.0 || portfolio.getCurrentBalance() < stake) {
                log.warn("Portefølje {} har ikke nok midler for å plassere innsats på {:.2f}", portfolio.getName(), stake);
                return;
            }

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
            newBet.setPlacedAt(Instant.now());

            placedBetRepository.save(newBet);

            portfolio.setCurrentBalance(portfolio.getCurrentBalance() - stake);
            portfolio.setTotalBets(portfolio.getTotalBets() + 1);

            log.info("PLACED BET: Portefølje '{}' plasserte '{}' på '{}' for kamp {}. Stake: {:.2f}, Odds: {:.2f}, Value: {:.2f}%",
                    portfolio.getName(), market, bestSelection, valueBet.getFixtureId(), stake, bestOdds, bestValue * 100);
        }
    }

    private boolean isFinite(double value) {
        return Double.isFinite(value) && value > 0;
    }
}