// src/main/java/com/AiPortal/service/OddsCalculationService.java
package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.InjuryRepository;
import com.AiPortal.repository.MatchOddsRepository;
import com.AiPortal.repository.MatchStatisticsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OddsCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OddsCalculationService.class);
    private static final int FORM_MATCH_COUNT = 10;

    private final MatchOddsRepository oddsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;
    private final InjuryRepository injuryRepository;
    private final PredictionService predictionService; // Ny avhengighet

    public OddsCalculationService(
            MatchOddsRepository oddsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository,
            InjuryRepository injuryRepository,
            PredictionService predictionService) { // Oppdatert konstruktør
        this.oddsRepository = oddsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.predictionService = predictionService;
    }

    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture) {
        // Denne metoden returnerer en liste, selv om vi foreløpig kun lager ett DTO for "Match Winner".
        // Dette klargjør for fremtidige utvidelser (f.eks. sentiment-signaler for andre markeder).
        return calculateMatchWinnerValue(fixture)
                .map(List::of)
                .orElse(Collections.emptyList());
    }

    private Optional<ValueBetDto> calculateMatchWinnerValue(Fixture fixture) {
        Optional<MatchOdds> marketOddsOpt = oddsRepository.findTopByFixtureId(fixture.getId());
        if (marketOddsOpt.isEmpty()) {
            log.warn("Kamp ID {}: Ingen markedsodds funnet, kan ikke beregne verdi.", fixture.getId());
            return Optional.empty();
        }

        // Prioritet 1: Prøv å hente prediksjon fra den trente ML-modellen.
        Optional<PredictionService.MLProbabilities> mlProbsOpt = getProbabilitiesFromMLModel(fixture);

        if (mlProbsOpt.isPresent()) {
            log.info("Kamp ID {}: Suksess! Bruker prediksjon fra ML-modell.", fixture.getId());
            PredictionService.MLProbabilities probs = mlProbsOpt.get();
            return Optional.of(buildValueBetDto(fixture, marketOddsOpt.get(), probs.homeWin, probs.draw, probs.awayWin, "ML Predictor v1"));
        } else {
            log.warn("Kamp ID {}: Kunne ikke hente prediksjon fra ML-modell for kamp {}.", fixture.getId(), fixture.getHomeTeamName());
            // For nå returnerer vi tom hvis ML-modellen feiler, for å tydelig se når den ikke fungerer.
            // En fremtidig forbedring er å falle tilbake på en statistisk modell her.
            return Optional.empty();
        }
    }

    private Optional<PredictionService.MLProbabilities> getProbabilitiesFromMLModel(Fixture fixture) {
        // Bygg feature-settet for hjemme- og bortelag
        TeamFeatureSet homeFeatures = calculateFeaturesForTeam(fixture.getHomeTeamId(), fixture);
        TeamFeatureSet awayFeatures = calculateFeaturesForTeam(fixture.getAwayTeamId(), fixture);

        // Lag et Map som matcher JSON-strukturen for prediksjons-APIet
        Map<String, Object> features = new HashMap<>();
        features.put("homeAvgShotsOnGoal", homeFeatures.avgShotsOnGoal);
        features.put("homeAvgShotsOffGoal", homeFeatures.avgShotsOffGoal);
        features.put("homeAvgCorners", homeFeatures.avgCorners);
        features.put("homeInjuries", homeFeatures.injuryCount);
        features.put("awayAvgShotsOnGoal", awayFeatures.avgShotsOnGoal);
        features.put("awayAvgShotsOffGoal", awayFeatures.avgShotsOffGoal);
        features.put("awayAvgCorners", awayFeatures.avgCorners);
        features.put("awayInjuries", awayFeatures.injuryCount);

        // Kall den nye PredictionService
        return predictionService.getMatchOutcomeProbabilities(features);
    }

    private TeamFeatureSet calculateFeaturesForTeam(Integer teamId, Fixture contextFixture) {
        List<Fixture> lastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamBeforeDate(
                teamId, contextFixture.getDate(), PageRequest.of(0, FORM_MATCH_COUNT)
        );
        if (lastFixtures.isEmpty()) {
            return new TeamFeatureSet(); // Returner tomt objekt hvis ingen historikk finnes
        }
        List<Long> fixtureIds = lastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        List<MatchStatistics> stats = matchStatsRepository.findAllByFixtureIdIn(fixtureIds);

        double avgShotsOnGoal = stats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOnGoal() != null).mapToDouble(MatchStatistics::getShotsOnGoal).average().orElse(0.0);
        double avgShotsOffGoal = stats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOffGoal() != null).mapToDouble(MatchStatistics::getShotsOffGoal).average().orElse(0.0);
        double avgCorners = stats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getCornerKicks() != null).mapToDouble(MatchStatistics::getCornerKicks).average().orElse(0.0);

        int injuryCount = injuryRepository.countByFixtureIdAndTeamId(contextFixture.getId(), teamId);

        return new TeamFeatureSet(avgShotsOnGoal, avgShotsOffGoal, avgCorners, injuryCount);
    }

    private static class TeamFeatureSet {
        double avgShotsOnGoal = 0.0;
        double avgShotsOffGoal = 0.0;
        double avgCorners = 0.0;
        int injuryCount = 0;
        TeamFeatureSet() {}
        TeamFeatureSet(double avgSot, double avgSotOff, double avgCorn, int injuries) {
            this.avgShotsOnGoal = avgSot;
            this.avgShotsOffGoal = avgSotOff;
            this.avgCorners = avgCorn;
            this.injuryCount = injuries;
        }
    }

    private ValueBetDto buildValueBetDto(Fixture fixture, MatchOdds marketOdds, double homeProb, double drawProb, double awayProb, String modelUsed) {
        ValueBetDto valueBet = new ValueBetDto();
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
        valueBet.setMarketDescription(modelUsed);
        valueBet.setMarketHomeOdds(marketOdds.getHomeOdds());
        valueBet.setMarketDrawOdds(marketOdds.getDrawOdds());
        valueBet.setMarketAwayOdds(marketOdds.getAwayOdds());
        if (marketOdds.getBookmaker() != null) {
            valueBet.setBookmakerName(marketOdds.getBookmaker().getName());
        }
        valueBet.setAracanixHomeOdds(homeProb > 0 ? 1 / homeProb : Double.POSITIVE_INFINITY);
        valueBet.setAracanixDrawOdds(drawProb > 0 ? 1 / drawProb : Double.POSITIVE_INFINITY);
        valueBet.setAracanixAwayOdds(awayProb > 0 ? 1 / awayProb : Double.POSITIVE_INFINITY);
        valueBet.setValueHome((marketOdds.getHomeOdds() * homeProb) - 1);
        valueBet.setValueDraw((marketOdds.getDrawOdds() * drawProb) - 1);
        valueBet.setValueAway((marketOdds.getAwayOdds() * awayProb) - 1);
        return valueBet;
    }
}