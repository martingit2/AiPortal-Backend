// src/main/java/com/AiPortal/service/OddsCalculationService.java
package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OddsCalculationService {

    private static final Logger log = LoggerFactory.getLogger(OddsCalculationService.class);
    private static final int FORM_MATCH_COUNT = 10;

    // Konstanter for standardmodeller som skal brukes i den generelle analysen
    private static final String DEFAULT_MATCH_WINNER_MODEL = "football_predictor_v5_h2h.joblib";
    private static final String DEFAULT_OVER_UNDER_MODEL = "over_under_v3_possession.joblib";

    private final MatchOddsRepository oddsRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;
    private final InjuryRepository injuryRepository;
    private final PredictionService predictionService;
    private final PlayerMatchStatisticsRepository playerMatchStatsRepository;
    private final HeadToHeadStatsRepository h2hStatsRepository;
    private final ObjectMapper objectMapper;

    public OddsCalculationService(
            MatchOddsRepository oddsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository,
            InjuryRepository injuryRepository,
            PredictionService predictionService,
            PlayerMatchStatisticsRepository playerMatchStatsRepository,
            HeadToHeadStatsRepository h2hStatsRepository,
            ObjectMapper objectMapper
    ) {
        this.oddsRepository = oddsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.predictionService = predictionService;
        this.playerMatchStatsRepository = playerMatchStatsRepository;
        this.h2hStatsRepository = h2hStatsRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * NY, OVERLASTET METODE: Kjører en generell analyse med standardmodeller.
     * Denne metoden brukes av ValueBetsController for å gi en generell oversikt i UI.
     * Den fikser kompileringsfeilen ved å tilby den "gamle" metodesignaturen.
     *
     * @param fixture Kampen som skal analyseres.
     * @return En liste med alle funnede verdispill fra alle standardmodeller.
     */
    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture) {
        log.debug("Kjører generell analyse for fixture {}", fixture.getId());
        List<ValueBetDto> allValueBets = new ArrayList<>();

        // Kjør analyse for Kampvinner
        allValueBets.addAll(calculateValue(fixture, DEFAULT_MATCH_WINNER_MODEL, "MATCH_WINNER"));

        // Kjør analyse for Over/Under
        allValueBets.addAll(calculateValue(fixture, DEFAULT_OVER_UNDER_MODEL, "OVER_UNDER_2.5"));

        return allValueBets;
    }

    /**
     * REFAKTORERT METODE: Utfører en modell-spesifikk analyse.
     * Denne metoden er nå kjernen i den modell-spesifikke dataflyten.
     *
     * @param fixture    Kampen som skal analyseres.
     * @param modelName  Filnavnet til modellen som skal brukes.
     * @param marketType Typen marked modellen er trent for.
     * @return En liste med funnede verdispill (typisk 0 eller 1 per kall).
     */
    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture, String modelName, String marketType) {
        Map<String, Object> features = buildFeatures(fixture);
        if (features.isEmpty()) {
            log.warn("Kunne ikke bygge features for fixture {}. Kan ikke analysere.", fixture.getId());
            return Collections.emptyList();
        }

        Optional<JsonNode> predictionResponse = predictionService.getPredictions(modelName, features);

        if (predictionResponse.isEmpty()) {
            log.warn("Mottok ingen prediksjon fra ML-tjenesten for fixture {} med modell {}", fixture.getId(), modelName);
            return Collections.emptyList();
        }

        JsonNode probabilitiesNode = predictionResponse.get().path("probabilities");
        if (probabilitiesNode.isMissingNode()) {
            log.error("JSON-respons fra ML-tjeneste mangler 'probabilities'-nøkkel for fixture {}", fixture.getId());
            return Collections.emptyList();
        }

        List<MatchOdds> allOddsForFixture = oddsRepository.findAllByFixtureId(fixture.getId());
        if (allOddsForFixture.isEmpty()) {
            return Collections.emptyList();
        }

        if ("MATCH_WINNER".equalsIgnoreCase(marketType)) {
            return allOddsForFixture.stream()
                    .filter(o -> "Match Winner".equalsIgnoreCase(o.getBetName()))
                    .findFirst()
                    .map(marketOdds -> buildMatchWinnerValueBetDto(fixture, marketOdds, probabilitiesNode))
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }

        if ("OVER_UNDER_2.5".equalsIgnoreCase(marketType)) {
            return allOddsForFixture.stream()
                    .filter(o -> "Total Goals".equalsIgnoreCase(o.getBetName()))
                    .findFirst()
                    .map(marketOdds -> buildOverUnderValueBetDto(fixture, marketOdds, probabilitiesNode))
                    .map(List::of)
                    .orElse(Collections.emptyList());
        }

        log.warn("Ukjent marketType '{}' mottatt for analyse av fixture {}", marketType, fixture.getId());
        return Collections.emptyList();
    }

    private ValueBetDto buildMatchWinnerValueBetDto(Fixture fixture, MatchOdds marketOdds, JsonNode probsNode) {
        ValueBetDto valueBet = new ValueBetDto();
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
        valueBet.setMarketDescription("Kampvinner");
        valueBet.setBookmakerName(marketOdds.getBookmaker() != null ? marketOdds.getBookmaker().getName() : "Ukjent");

        try {
            JsonNode oddsData = objectMapper.readTree(marketOdds.getOddsData());
            for (JsonNode value : oddsData) {
                switch (value.path("name").asText()) {
                    case "Home": valueBet.setMarketHomeOdds(value.path("odds").asDouble()); break;
                    case "Draw": valueBet.setMarketDrawOdds(value.path("odds").asDouble()); break;
                    case "Away": valueBet.setMarketAwayOdds(value.path("odds").asDouble()); break;
                }
            }
        } catch (Exception e) {
            log.error("Kunne ikke parse kampvinner-oddsData for fixture {}: {}", fixture.getId(), e.getMessage());
        }

        // *** OPPDATERT LOGIKK ***
        // Tolker den standardiserte "class_N"-responsen fra Python.
        // VIKTIG: Denne rekkefølgen må matche rekkefølgen til LabelEncoder i Python.
        // Standard er alfabetisk: AWAY_WIN (0), DRAW (1), HOME_WIN (2).
        double probAway = probsNode.path("class_0").asDouble(0.0);
        double probDraw = probsNode.path("class_1").asDouble(0.0);
        double probHome = probsNode.path("class_2").asDouble(0.0);

        valueBet.setAracanixHomeOdds(probHome > 0 ? 1 / probHome : Double.POSITIVE_INFINITY);
        valueBet.setAracanixDrawOdds(probDraw > 0 ? 1 / probDraw : Double.POSITIVE_INFINITY);
        valueBet.setAracanixAwayOdds(probAway > 0 ? 1 / probAway : Double.POSITIVE_INFINITY);
        valueBet.setValueHome((valueBet.getMarketHomeOdds() * probHome) - 1);
        valueBet.setValueDraw((valueBet.getMarketDrawOdds() * probDraw) - 1);
        valueBet.setValueAway((valueBet.getMarketAwayOdds() * probAway) - 1);

        return valueBet;
    }

    private ValueBetDto buildOverUnderValueBetDto(Fixture fixture, MatchOdds marketOdds, JsonNode probsNode) {
        ValueBetDto valueBet = new ValueBetDto();
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
        valueBet.setMarketDescription("Over/Under 2.5");
        valueBet.setBookmakerName(marketOdds.getBookmaker() != null ? marketOdds.getBookmaker().getName() : "Ukjent");

        try {
            JsonNode oddsData = objectMapper.readTree(marketOdds.getOddsData());
            for (JsonNode value : oddsData) {
                if ("2.5".equals(value.path("points").asText())) {
                    if ("Over".equalsIgnoreCase(value.path("name").asText())) {
                        valueBet.setMarketHomeOdds(value.path("odds").asDouble());
                    } else if ("Under".equalsIgnoreCase(value.path("name").asText())) {
                        valueBet.setMarketAwayOdds(value.path("odds").asDouble());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Kunne ikke parse O/U-oddsData for fixture {}: {}", fixture.getId(), e.getMessage());
        }

        // For binære modeller: class_0 er Under, class_1 er Over.
        double probUnder = probsNode.path("class_0").asDouble(0.0);
        double probOver = probsNode.path("class_1").asDouble(0.0);

        valueBet.setAracanixHomeOdds(probOver > 0 ? 1 / probOver : Double.POSITIVE_INFINITY); // Home = Over
        valueBet.setAracanixAwayOdds(probUnder > 0 ? 1 / probUnder : Double.POSITIVE_INFINITY); // Away = Under
        valueBet.setValueHome((valueBet.getMarketHomeOdds() * probOver) - 1); // ValueHome = Over
        valueBet.setValueAway((valueBet.getMarketAwayOdds() * probUnder) - 1); // ValueAway = Under

        return valueBet;
    }

    private Map<String, Object> buildFeatures(Fixture fixture) {
        TeamFeatureSet homeFeatures = calculateFeaturesForTeam(fixture.getHomeTeamId(), fixture);
        TeamFeatureSet awayFeatures = calculateFeaturesForTeam(fixture.getAwayTeamId(), fixture);

        if (homeFeatures.isEmpty || awayFeatures.isEmpty) {
            return Collections.emptyMap();
        }

        Map<String, Object> features = new HashMap<>();
        features.put("homeAvgShotsOnGoal", homeFeatures.avgShotsOnGoal);
        features.put("homeAvgShotsOffGoal", homeFeatures.avgShotsOffGoal);
        features.put("homeAvgCorners", homeFeatures.avgCorners);
        features.put("homeInjuries", homeFeatures.injuryCount);
        features.put("homePlayersAvgRating", homeFeatures.avgPlayerRating);
        features.put("homePlayersAvgGoals", homeFeatures.avgPlayerGoals);
        features.put("homeAvgPossession", homeFeatures.avgPossession);
        features.put("awayAvgShotsOnGoal", awayFeatures.avgShotsOnGoal);
        features.put("awayAvgShotsOffGoal", awayFeatures.avgShotsOffGoal);
        features.put("awayAvgCorners", awayFeatures.avgCorners);
        features.put("awayInjuries", awayFeatures.injuryCount);
        features.put("awayPlayersAvgRating", awayFeatures.avgPlayerRating);
        features.put("awayPlayersAvgGoals", awayFeatures.avgPlayerGoals);
        features.put("awayAvgPossession", awayFeatures.avgPossession);

        h2hStatsRepository.findAllByFixtureIdIn(List.of(fixture.getId())).stream()
                .findFirst()
                .ifPresentOrElse(h2h -> {
                    if (h2h.getMatchesPlayed() > 0) {
                        features.put("h2hHomeWinPercentage", (double) h2h.getTeam1Wins() / h2h.getMatchesPlayed());
                        features.put("h2hAwayWinPercentage", (double) h2h.getTeam2Wins() / h2h.getMatchesPlayed());
                        features.put("h2hDrawPercentage", (double) h2h.getDraws() / h2h.getMatchesPlayed());
                        features.put("h2hAvgGoals", h2h.getAvgTotalGoals());
                    } else {
                        addDefaultH2hFeatures(features);
                    }
                }, () -> addDefaultH2hFeatures(features));

        return features;
    }

    private void addDefaultH2hFeatures(Map<String, Object> features) {
        features.put("h2hHomeWinPercentage", 0.0);
        features.put("h2hAwayWinPercentage", 0.0);
        features.put("h2hDrawPercentage", 0.0);
        features.put("h2hAvgGoals", 0.0);
    }

    private TeamFeatureSet calculateFeaturesForTeam(Integer teamId, Fixture contextFixture) {
        List<Fixture> lastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamBeforeDate(
                teamId, contextFixture.getDate(), PageRequest.of(0, FORM_MATCH_COUNT)
        );
        if (lastFixtures.isEmpty()) return new TeamFeatureSet(true);

        List<Long> fixtureIds = lastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        List<MatchStatistics> teamStats = matchStatsRepository.findAllByFixtureIdIn(fixtureIds);
        List<PlayerMatchStatistics> playerStats = playerMatchStatsRepository.findAllByFixtureIdIn(fixtureIds);

        double avgSot = teamStats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOnGoal() != null).mapToDouble(MatchStatistics::getShotsOnGoal).average().orElse(0.0);
        double avgSotOff = teamStats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOffGoal() != null).mapToDouble(MatchStatistics::getShotsOffGoal).average().orElse(0.0);
        double avgCorn = teamStats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getCornerKicks() != null).mapToDouble(MatchStatistics::getCornerKicks).average().orElse(0.0);
        double avgPoss = teamStats.stream()
                .filter(s -> s.getTeamId().equals(teamId) && s.getBallPossession() != null)
                .mapToDouble(s -> {
                    try { return Double.parseDouble(s.getBallPossession().replace("%", "")); } catch (Exception e) { return 0.0; }
                })
                .average().orElse(0.0);
        double avgRating = playerStats.stream()
                .filter(ps -> ps.getTeamId().equals(teamId) && ps.getRating() != null)
                .mapToDouble(ps -> { try { return Double.parseDouble(ps.getRating()); } catch (Exception e) { return 0.0; } })
                .average().orElse(0.0);
        double avgGoals = playerStats.stream()
                .filter(ps -> ps.getTeamId().equals(teamId) && ps.getGoalsTotal() != null)
                .mapToDouble(PlayerMatchStatistics::getGoalsTotal).average().orElse(0.0);
        int injuries = injuryRepository.countByFixtureIdAndTeamId(contextFixture.getId(), teamId);

        return new TeamFeatureSet(avgSot, avgSotOff, avgCorn, injuries, avgRating, avgGoals, avgPoss);
    }

    private static class TeamFeatureSet {
        double avgShotsOnGoal, avgShotsOffGoal, avgCorners, avgPlayerRating, avgPlayerGoals, avgPossession;
        int injuryCount;
        boolean isEmpty = false;

        TeamFeatureSet(boolean isEmpty) { this.isEmpty = isEmpty; }

        TeamFeatureSet(double avgSot, double avgSotOff, double avgCorn, int injuries, double avgRating, double avgGoals, double avgPoss) {
            this.avgShotsOnGoal = avgSot;
            this.avgShotsOffGoal = avgSotOff;
            this.avgCorners = avgCorn;
            this.injuryCount = injuries;
            this.avgPlayerRating = avgRating;
            this.avgPlayerGoals = avgGoals;
            this.avgPossession = avgPoss;
        }
    }
}