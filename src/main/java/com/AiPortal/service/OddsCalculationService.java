// src/main/java/com/AiPortal/service/OddsCalculationService.java
package com.AiPortal.service;

import com.AiPortal.dto.ValueBetDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.MatchOdds;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.PlayerMatchStatistics;
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
    private final ObjectMapper objectMapper;

    public OddsCalculationService(
            MatchOddsRepository oddsRepository,
            FixtureRepository fixtureRepository,
            MatchStatisticsRepository matchStatsRepository,
            InjuryRepository injuryRepository,
            PredictionService predictionService,
            PlayerMatchStatisticsRepository playerMatchStatsRepository,
            ObjectMapper objectMapper
    ) {
        this.oddsRepository = oddsRepository;
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.predictionService = predictionService;
        this.playerMatchStatsRepository = playerMatchStatsRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<ValueBetDto> calculateValue(Fixture fixture) {
        List<MatchOdds> allOddsForFixture = oddsRepository.findAllByFixtureId(fixture.getId());
        if (allOddsForFixture.isEmpty()) {
            log.warn("Kamp ID {}: Ingen markedsodds funnet, kan ikke beregne verdi.", fixture.getId());
            return Collections.emptyList();
        }

        // --- NYTT: Lag features én gang, gjenbruk for alle modeller ---
        Map<String, Object> features = buildFeatures(fixture);
        if (features.isEmpty()) {
            log.warn("Kamp ID {}: Kunne ikke bygge features, hopper over analyse.", fixture.getId());
            return Collections.emptyList();
        }
        // -----------------------------------------------------------------

        List<ValueBetDto> foundValueBets = new ArrayList<>();

        for (MatchOdds marketOdds : allOddsForFixture) {
            String betName = marketOdds.getBetName();

            // Logikk for å velge riktig analyse basert på markedets navn
            if ("Match Winner".equalsIgnoreCase(betName)) {
                calculateMatchWinnerValue(fixture, marketOdds, features).ifPresent(foundValueBets::add);
            } else if ("Total Goals".equalsIgnoreCase(betName)) {
                calculateOverUnderValue(fixture, marketOdds, features).ifPresent(foundValueBets::add);
            }
            // ... kan legge til 'else if' for flere markeder her i fremtiden
        }

        return foundValueBets;
    }

    private Optional<ValueBetDto> calculateMatchWinnerValue(Fixture fixture, MatchOdds marketOdds, Map<String, Object> features) {
        Optional<PredictionService.MLProbabilities> mlProbsOpt = predictionService.getMatchOutcomeProbabilities(features);

        if (mlProbsOpt.isPresent()) {
            log.info("Kamp ID {}: Suksess! Bruker prediksjon fra kampvinner-modell.", fixture.getId());
            PredictionService.MLProbabilities probs = mlProbsOpt.get();
            return Optional.of(buildMatchWinnerValueBetDto(fixture, marketOdds, probs));
        } else {
            log.warn("Kamp ID {}: Kunne ikke hente prediksjon fra kampvinner-modell.", fixture.getId());
            return Optional.empty();
        }
    }

    // --- NY METODE for å kalkulere verdi i Over/Under-markedet ---
    private Optional<ValueBetDto> calculateOverUnderValue(Fixture fixture, MatchOdds marketOdds, Map<String, Object> features) {
        Optional<PredictionService.OverUnderProbabilities> mlProbsOpt = predictionService.getOverUnderProbabilities(features);

        if (mlProbsOpt.isPresent()) {
            log.info("Kamp ID {}: Suksess! Bruker prediksjon fra Over/Under-modell.", fixture.getId());
            PredictionService.OverUnderProbabilities probs = mlProbsOpt.get();
            return Optional.of(buildOverUnderValueBetDto(fixture, marketOdds, probs));
        } else {
            log.warn("Kamp ID {}: Kunne ikke hente prediksjon fra Over/Under-modell.", fixture.getId());
            return Optional.empty();
        }
    }
    // ----------------------------------------------------------------

    // Bygger et "feature map" som kan sendes til hvilken som helst modell
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

        features.put("awayAvgShotsOnGoal", awayFeatures.avgShotsOnGoal);
        features.put("awayAvgShotsOffGoal", awayFeatures.avgShotsOffGoal);
        features.put("awayAvgCorners", awayFeatures.avgCorners);
        features.put("awayInjuries", awayFeatures.injuryCount);
        features.put("awayPlayersAvgRating", awayFeatures.avgPlayerRating);
        features.put("awayPlayersAvgGoals", awayFeatures.avgPlayerGoals);

        return features;
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

        double avgRating = playerStats.stream()
                .filter(ps -> ps.getTeamId().equals(teamId) && ps.getRating() != null)
                .mapToDouble(ps -> { try { return Double.parseDouble(ps.getRating()); } catch (Exception e) { return 0.0; } })
                .average().orElse(0.0);

        double avgGoals = playerStats.stream()
                .filter(ps -> ps.getTeamId().equals(teamId) && ps.getGoalsTotal() != null)
                .mapToDouble(PlayerMatchStatistics::getGoalsTotal).average().orElse(0.0);

        int injuries = injuryRepository.countByFixtureIdAndTeamId(contextFixture.getId(), teamId);

        return new TeamFeatureSet(avgSot, avgSotOff, avgCorn, injuries, avgRating, avgGoals);
    }

    private static class TeamFeatureSet {
        double avgShotsOnGoal, avgShotsOffGoal, avgCorners, avgPlayerRating, avgPlayerGoals;
        int injuryCount;
        boolean isEmpty = false;

        TeamFeatureSet(boolean isEmpty) { this.isEmpty = isEmpty; }
        TeamFeatureSet(double avgSot, double avgSotOff, double avgCorn, int injuries, double avgRating, double avgGoals) {
            this.avgShotsOnGoal = avgSot; this.avgShotsOffGoal = avgSotOff; this.avgCorners = avgCorn;
            this.injuryCount = injuries; this.avgPlayerRating = avgRating; this.avgPlayerGoals = avgGoals;
        }
    }

    // Eksisterende metode for å bygge DTO for kampvinner
    private ValueBetDto buildMatchWinnerValueBetDto(Fixture fixture, MatchOdds marketOdds, PredictionService.MLProbabilities probs) {
        ValueBetDto valueBet = new ValueBetDto();
        // ... (Fyller inn felles info som før) ...
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
        valueBet.setMarketDescription("Kampvinner (ML v3)");
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

    // --- NY METODE for å bygge DTO for Over/Under ---
    private ValueBetDto buildOverUnderValueBetDto(Fixture fixture, MatchOdds marketOdds, PredictionService.OverUnderProbabilities probs) {
        ValueBetDto valueBet = new ValueBetDto();
        valueBet.setFixtureId(fixture.getId());
        valueBet.setHomeTeamName(fixture.getHomeTeamName());
        valueBet.setAwayTeamName(fixture.getAwayTeamName());
        valueBet.setFixtureDate(fixture.getDate());
        valueBet.setMarketDescription("Over/Under 2.5 (ML v1)");
        valueBet.setBookmakerName(marketOdds.getBookmaker() != null ? marketOdds.getBookmaker().getName() : "Ukjent");

        try {
            JsonNode oddsData = objectMapper.readTree(marketOdds.getOddsData());
            for (JsonNode value : oddsData) {
                // Vi ser kun på O/U 2.5 foreløpig
                if ("2.5".equals(value.path("points").asText())) {
                    if ("Over".equalsIgnoreCase(value.path("name").asText())) {
                        valueBet.setMarketHomeOdds(value.path("odds").asDouble()); // Gjenbruker Home-feltet for "Over"
                    } else if ("Under".equalsIgnoreCase(value.path("name").asText())) {
                        valueBet.setMarketAwayOdds(value.path("odds").asDouble()); // Gjenbruker Away-feltet for "Under"
                    }
                }
            }
        } catch (Exception e) {
            log.error("Kunne ikke parse O/U-oddsData for fixture {}: {}", fixture.getId(), e.getMessage());
        }

        // Våre beregnede odds
        valueBet.setAracanixHomeOdds(probs.over > 0 ? 1 / probs.over : Double.POSITIVE_INFINITY); // Gjenbruker Home for "Over"
        valueBet.setAracanixAwayOdds(probs.under > 0 ? 1 / probs.under : Double.POSITIVE_INFINITY); // Gjenbruker Away for "Under"

        // Value-beregning
        valueBet.setValueHome((valueBet.getMarketHomeOdds() * probs.over) - 1); // Gjenbruker Home for "Over"
        valueBet.setValueAway((valueBet.getMarketAwayOdds() * probs.under) - 1); // Gjenbruker Away for "Under"

        return valueBet;
    }
}