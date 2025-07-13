// src/main/java/com/AiPortal/service/OddsCalculationService.java
package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.PlayerMatchStatistics; // Importer
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.InjuryRepository;
import com.AiPortal.repository.MatchOddsRepository;
import com.AiPortal.repository.MatchStatisticsRepository;
import com.AiPortal.repository.PlayerMatchStatisticsRepository; // Importer

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PredictionService predictionService;
    private final PlayerMatchStatisticsRepository playerMatchStatsRepository; // Ny avhengighet

    public OddsCalculationService(
            MatchOddsRepository oddsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository,
            InjuryRepository injuryRepository,
            PredictionService predictionService,
            PlayerMatchStatisticsRepository playerMatchStatsRepository) { // Oppdatert konstruktør
        this.oddsRepository = oddsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.predictionService = predictionService;
        this.playerMatchStatsRepository = playerMatchStatsRepository; // Ny avhengighet
    }

    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture) {
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

        Optional<PredictionService.MLProbabilities> mlProbsOpt = getProbabilitiesFromMLModel(fixture);

        if (mlProbsOpt.isPresent()) {
            log.info("Kamp ID {}: Suksess! Bruker prediksjon fra ML-modell.", fixture.getId());
            PredictionService.MLProbabilities probs = mlProbsOpt.get();
            return Optional.of(buildValueBetDto(fixture, marketOddsOpt.get(), probs.homeWin, probs.draw, probs.awayWin, "ML Predictor v2 (Spillerdata)"));
        } else {
            log.warn("Kamp ID {}: Kunne ikke hente prediksjon fra ML-modell for kamp {}.", fixture.getId(), fixture.getHomeTeamName());
            return Optional.empty();
        }
    }

    private Optional<PredictionService.MLProbabilities> getProbabilitiesFromMLModel(Fixture fixture) {
        TeamFeatureSet homeFeatures = calculateFeaturesForTeam(fixture.getHomeTeamId(), fixture);
        TeamFeatureSet awayFeatures = calculateFeaturesForTeam(fixture.getAwayTeamId(), fixture);

        Map<String, Object> features = new HashMap<>();
        features.put("homeAvgShotsOnGoal", homeFeatures.avgShotsOnGoal);
        features.put("homeAvgShotsOffGoal", homeFeatures.avgShotsOffGoal);
        features.put("homeAvgCorners", homeFeatures.avgCorners);
        features.put("homeInjuries", homeFeatures.injuryCount);
        features.put("homePlayersAvgRating", homeFeatures.avgPlayerRating);
        features.put("homePlayersAvgGoals", homeFeatures.avgPlayerGoals);

        features.put("awayAvgShotsOnGoal", awayFeatures.avgShotsOnGoal);
        features.put("awayAvgShotsOffGoal", awayFeatures.avgShotsOffGoal);
        features.put("awayAvgCorners", awayFeatures.avgCorners);
        features.put("awayInjuries", awayFeatures.injuryCount);
        features.put("awayPlayersAvgRating", awayFeatures.avgPlayerRating);
        features.put("awayPlayersAvgGoals", awayFeatures.avgPlayerGoals);

        return predictionService.getMatchOutcomeProbabilities(features);
    }

    private TeamFeatureSet calculateFeaturesForTeam(Integer teamId, Fixture contextFixture) {
        List<Fixture> lastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamBeforeDate(
                teamId, contextFixture.getDate(), PageRequest.of(0, FORM_MATCH_COUNT)
        );
        if (lastFixtures.isEmpty()) {
            return new TeamFeatureSet();
        }
        List<Long> fixtureIds = lastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        // Hent lag-statistikk
        List<MatchStatistics> teamStats = matchStatsRepository.findAllByFixtureIdIn(fixtureIds);
        double avgShotsOnGoal = teamStats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOnGoal() != null).mapToDouble(MatchStatistics::getShotsOnGoal).average().orElse(0.0);
        double avgShotsOffGoal = teamStats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOffGoal() != null).mapToDouble(MatchStatistics::getShotsOffGoal).average().orElse(0.0);
        double avgCorners = teamStats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getCornerKicks() != null).mapToDouble(MatchStatistics::getCornerKicks).average().orElse(0.0);

        // --- VIKTIG ENDRING: Hent og beregn spiller-statistikk ---
        List<PlayerMatchStatistics> playerStats = playerMatchStatsRepository.findAllByFixtureIdIn(fixtureIds);

        double avgPlayerRating = playerStats.stream()
                .filter(ps -> ps.getTeamId().equals(teamId) && ps.getRating() != null)
                .mapToDouble(ps -> {
                    try { return Double.parseDouble(ps.getRating()); } catch (NumberFormatException e) { return 0.0; }
                })
                .average()
                .orElse(0.0);

        double avgPlayerGoals = playerStats.stream()
                .filter(ps -> ps.getTeamId().equals(teamId) && ps.getGoalsTotal() != null)
                .mapToDouble(PlayerMatchStatistics::getGoalsTotal)
                .average()
                .orElse(0.0);

        int injuryCount = injuryRepository.countByFixtureIdAndTeamId(contextFixture.getId(), teamId);

        return new TeamFeatureSet(avgShotsOnGoal, avgShotsOffGoal, avgCorners, injuryCount, avgPlayerRating, avgPlayerGoals);
    }

    private static class TeamFeatureSet {
        double avgShotsOnGoal = 0.0;
        double avgShotsOffGoal = 0.0;
        double avgCorners = 0.0;
        int injuryCount = 0;
        double avgPlayerRating = 0.0;
        double avgPlayerGoals = 0.0;

        TeamFeatureSet() {}

        TeamFeatureSet(double avgSot, double avgSotOff, double avgCorn, int injuries, double avgRating, double avgGoals) {
            this.avgShotsOnGoal = avgSot;
            this.avgShotsOffGoal = avgSotOff;
            this.avgCorners = avgCorn;
            this.injuryCount = injuries;
            this.avgPlayerRating = avgRating;
            this.avgPlayerGoals = avgGoals;
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