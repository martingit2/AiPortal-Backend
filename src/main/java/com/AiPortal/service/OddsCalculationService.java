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

    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture) {
        List<MatchOdds> allOddsForFixture = oddsRepository.findAllByFixtureId(fixture.getId());
        if (allOddsForFixture.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> features = buildFeatures(fixture);
        if (features.isEmpty()) {
            return Collections.emptyList();
        }

        List<ValueBetDto> foundValueBets = new ArrayList<>();

        // Kjør kampvinner-analyse
        predictionService.getMatchOutcomeProbabilities(features)
                .flatMap(probs ->
                        allOddsForFixture.stream()
                                .filter(o -> "Match Winner".equalsIgnoreCase(o.getBetName()))
                                .findFirst()
                                .map(marketOdds -> buildMatchWinnerValueBetDto(fixture, marketOdds, probs))
                )
                .ifPresent(foundValueBets::add);

        // Kjør Over/Under-analyse
        predictionService.getOverUnderProbabilities(features)
                .flatMap(probs ->
                        allOddsForFixture.stream()
                                .filter(o -> "Total Goals".equalsIgnoreCase(o.getBetName()))
                                .findFirst()
                                .map(marketOdds -> buildOverUnderValueBetDto(fixture, marketOdds, probs))
                )
                .ifPresent(foundValueBets::add);

        return foundValueBets;
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

    private ValueBetDto buildMatchWinnerValueBetDto(Fixture fixture, MatchOdds marketOdds, PredictionService.MLProbabilities probs) {
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

        valueBet.setAracanixHomeOdds(probs.homeWin > 0 ? 1 / probs.homeWin : Double.POSITIVE_INFINITY);
        valueBet.setAracanixDrawOdds(probs.draw > 0 ? 1 / probs.draw : Double.POSITIVE_INFINITY);
        valueBet.setAracanixAwayOdds(probs.awayWin > 0 ? 1 / probs.awayWin : Double.POSITIVE_INFINITY);
        valueBet.setValueHome((valueBet.getMarketHomeOdds() * probs.homeWin) - 1);
        valueBet.setValueDraw((valueBet.getMarketDrawOdds() * probs.draw) - 1);
        valueBet.setValueAway((valueBet.getMarketAwayOdds() * probs.awayWin) - 1);

        return valueBet;
    }

    private ValueBetDto buildOverUnderValueBetDto(Fixture fixture, MatchOdds marketOdds, PredictionService.OverUnderProbabilities probs) {
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

        valueBet.setAracanixHomeOdds(probs.over > 0 ? 1 / probs.over : Double.POSITIVE_INFINITY);
        valueBet.setAracanixAwayOdds(probs.under > 0 ? 1 / probs.under : Double.POSITIVE_INFINITY);

        valueBet.setValueHome((valueBet.getMarketHomeOdds() * probs.over) - 1);
        valueBet.setValueAway((valueBet.getMarketAwayOdds() * probs.under) - 1);

        return valueBet;
    }

    private static class TeamFeatureSet {
        double avgShotsOnGoal, avgShotsOffGoal, avgCorners, avgPlayerRating, avgPlayerGoals, avgPossession;
        int injuryCount;
        boolean isEmpty = false;

        TeamFeatureSet(boolean isEmpty) { this.isEmpty = isEmpty; }
        TeamFeatureSet() {}

        TeamFeatureSet(double avgSot, double avgSotOff, double avgCorn, int injuries, double avgRating, double avgGoals, double avgPoss) {
            this.avgShotsOnGoal = avgSot; this.avgShotsOffGoal = avgSotOff; this.avgCorners = avgCorn;
            this.injuryCount = injuries; this.avgPlayerRating = avgRating; this.avgPlayerGoals = avgGoals; this.avgPossession = avgPoss;
        }
    }
}