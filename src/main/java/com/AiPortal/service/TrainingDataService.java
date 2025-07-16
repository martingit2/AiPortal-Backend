// src/main/java/com/AiPortal/service/TrainingDataService.java
package com.AiPortal.service;

import com.AiPortal.dto.TrainingDataDto;
import com.AiPortal.entity.*;
import com.AiPortal.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TrainingDataService {

    private static final Logger log = LoggerFactory.getLogger(TrainingDataService.class);
    private static final int FORM_MATCH_COUNT_FOR_TRAINING = 10;
    private static final List<String> FINISHED_STATUSES = List.of("FT", "AET", "PEN");

    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;
    private final InjuryRepository injuryRepository;
    private final PlayerMatchStatisticsRepository playerMatchStatsRepository;
    private final HeadToHeadStatsRepository h2hStatsRepository;

    public TrainingDataService(FixtureRepository fixtureRepository,
                               MatchStatisticsRepository matchStatsRepository,
                               InjuryRepository injuryRepository,
                               PlayerMatchStatisticsRepository playerMatchStatsRepository,
                               HeadToHeadStatsRepository h2hStatsRepository) {
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.playerMatchStatsRepository = playerMatchStatsRepository;
        this.h2hStatsRepository = h2hStatsRepository;
    }

    public List<TrainingDataDto> buildTrainingSet() {
        log.info("--- [POSSESSION-FEATURE] Starter bygging av treningssett ---");

        List<Fixture> allCompletedFixtures = fixtureRepository.findByStatusIn(FINISHED_STATUSES);
        log.info("Fant {} ferdigspilte kamper.", allCompletedFixtures.size());
        if (allCompletedFixtures.isEmpty()) return Collections.emptyList();

        List<Long> allFixtureIds = allCompletedFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        Map<Long, List<MatchStatistics>> teamStatsByFixtureId = matchStatsRepository.findAllByFixtureIdIn(allFixtureIds).stream().collect(Collectors.groupingBy(MatchStatistics::getFixtureId));
        Map<Long, List<PlayerMatchStatistics>> playerStatsByFixtureId = playerMatchStatsRepository.findAllByFixtureIdIn(allFixtureIds).stream().collect(Collectors.groupingBy(PlayerMatchStatistics::getFixtureId));
        Map<Long, Map<Integer, Long>> injuriesByFixtureAndTeam = injuryRepository.findAllByFixtureIdIn(allFixtureIds).stream().collect(Collectors.groupingBy(Injury::getFixtureId, Collectors.groupingBy(Injury::getTeamId, Collectors.counting())));
        Map<Long, HeadToHeadStats> h2hByFixtureId = h2hStatsRepository.findAllByFixtureIdIn(allFixtureIds).stream().collect(Collectors.toMap(h2h -> h2h.getFixture().getId(), h2h -> h2h));

        Map<Integer, List<Fixture>> fixturesByTeamId = new HashMap<>();
        allCompletedFixtures.forEach(f -> {
            fixturesByTeamId.computeIfAbsent(f.getHomeTeamId(), k -> new ArrayList<>()).add(f);
            fixturesByTeamId.computeIfAbsent(f.getAwayTeamId(), k -> new ArrayList<>()).add(f);
        });
        fixturesByTeamId.values().forEach(list -> list.sort(Comparator.comparing(Fixture::getDate).reversed()));

        log.info("All data er hentet fra DB. Starter feature engineering i minnet.");

        List<TrainingDataDto> trainingSet = new ArrayList<>();
        for (Fixture fixture : allCompletedFixtures) {
            if (fixture.getGoalsHome() == null || fixture.getGoalsAway() == null) continue;

            TrainingDataDto dto = new TrainingDataDto();
            dto.setFixtureId(fixture.getId());
            dto.setLeagueId(fixture.getLeagueId());
            dto.setSeason(fixture.getSeason());

            TeamFeatureSet homeFeatures = calculateFeaturesForTeamInMemory(fixture.getHomeTeamId(), fixture, fixturesByTeamId, teamStatsByFixtureId, playerStatsByFixtureId, injuriesByFixtureAndTeam);
            dto.setHomeAvgShotsOnGoal(homeFeatures.avgShotsOnGoal);
            dto.setHomeAvgShotsOffGoal(homeFeatures.avgShotsOffGoal);
            dto.setHomeAvgCorners(homeFeatures.avgCorners);
            dto.setHomeInjuries(homeFeatures.injuryCount);
            dto.setHomePlayersAvgRating(homeFeatures.avgPlayerRating);
            dto.setHomePlayersAvgGoals(homeFeatures.avgPlayerGoals);
            dto.setHomeAvgPossession(homeFeatures.avgPossession); // <-- SETT NY FEATURE

            TeamFeatureSet awayFeatures = calculateFeaturesForTeamInMemory(fixture.getAwayTeamId(), fixture, fixturesByTeamId, teamStatsByFixtureId, playerStatsByFixtureId, injuriesByFixtureAndTeam);
            dto.setAwayAvgShotsOnGoal(awayFeatures.avgShotsOnGoal);
            dto.setAwayAvgShotsOffGoal(awayFeatures.avgShotsOffGoal);
            dto.setAwayAvgCorners(awayFeatures.avgCorners);
            dto.setAwayInjuries(awayFeatures.injuryCount);
            dto.setAwayPlayersAvgRating(awayFeatures.avgPlayerRating);
            dto.setAwayPlayersAvgGoals(awayFeatures.avgPlayerGoals);
            dto.setAwayAvgPossession(awayFeatures.avgPossession); // <-- SETT NY FEATURE

            HeadToHeadStats h2hStats = h2hByFixtureId.get(fixture.getId());
            if (h2hStats != null && h2hStats.getMatchesPlayed() > 0) {
                dto.setH2hHomeWinPercentage((double) h2hStats.getTeam1Wins() / h2hStats.getMatchesPlayed());
                dto.setH2hAwayWinPercentage((double) h2hStats.getTeam2Wins() / h2hStats.getMatchesPlayed());
                dto.setH2hDrawPercentage((double) h2hStats.getDraws() / h2hStats.getMatchesPlayed());
                dto.setH2hAvgGoals(h2hStats.getAvgTotalGoals());
            } else {
                dto.setH2hHomeWinPercentage(0.0);
                dto.setH2hAwayWinPercentage(0.0);
                dto.setH2hDrawPercentage(0.0);
                dto.setH2hAvgGoals(0.0);
            }

            if (fixture.getGoalsHome() > fixture.getGoalsAway()) dto.setResult("HOME_WIN");
            else if (fixture.getGoalsAway() > fixture.getGoalsHome()) dto.setResult("AWAY_WIN");
            else dto.setResult("DRAW");

            dto.setGoalsHome(fixture.getGoalsHome());
            dto.setGoalsAway(fixture.getGoalsAway());

            trainingSet.add(dto);
        }

        log.info("Fullførte bygging av treningssett med {} rader. Antall DB-kall: 5", trainingSet.size());
        return trainingSet;
    }

    private TeamFeatureSet calculateFeaturesForTeamInMemory(
            Integer teamId, Fixture contextFixture, Map<Integer, List<Fixture>> fixturesByTeam,
            Map<Long, List<MatchStatistics>> teamStatsByFixture, Map<Long, List<PlayerMatchStatistics>> playerStatsByFixture,
            Map<Long, Map<Integer, Long>> injuriesByFixture) {

        List<Fixture> pastFixtures = fixturesByTeam.getOrDefault(teamId, Collections.emptyList()).stream()
                .filter(f -> f.getDate().isBefore(contextFixture.getDate()))
                .limit(FORM_MATCH_COUNT_FOR_TRAINING)
                .collect(Collectors.toList());

        if (pastFixtures.isEmpty()) {
            return new TeamFeatureSet();
        }

        List<MatchStatistics> relevantTeamStats = pastFixtures.stream()
                .flatMap(f -> teamStatsByFixture.getOrDefault(f.getId(), Collections.emptyList()).stream())
                .filter(s -> s.getTeamId().equals(teamId))
                .collect(Collectors.toList());

        // Eksisterende beregninger
        double avgShotsOnGoal = relevantTeamStats.stream().mapToInt(s -> Optional.ofNullable(s.getShotsOnGoal()).orElse(0)).average().orElse(0.0);
        double avgShotsOffGoal = relevantTeamStats.stream().mapToInt(s -> Optional.ofNullable(s.getShotsOffGoal()).orElse(0)).average().orElse(0.0);
        double avgCorners = relevantTeamStats.stream().mapToInt(s -> Optional.ofNullable(s.getCornerKicks()).orElse(0)).average().orElse(0.0);

        // --- NY BEREGNING for Possession ---
        double avgPossession = relevantTeamStats.stream()
                .map(s -> {
                    String possessionStr = s.getBallPossession();
                    if (possessionStr == null || !possessionStr.contains("%")) return 0.0;
                    try {
                        return Double.parseDouble(possessionStr.replace("%", ""));
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        // ------------------------------------

        List<PlayerMatchStatistics> relevantPlayerStats = pastFixtures.stream()
                .flatMap(f -> playerStatsByFixture.getOrDefault(f.getId(), Collections.emptyList()).stream())
                .filter(ps -> ps.getTeamId().equals(teamId))
                .collect(Collectors.toList());

        double avgPlayerRating = relevantPlayerStats.stream()
                .map(ps -> {
                    try {
                        return ps.getRating() != null ? Double.parseDouble(ps.getRating()) : 0.0;
                    } catch (NumberFormatException e) { return 0.0; }
                })
                .mapToDouble(Double::doubleValue).average().orElse(0.0);

        double avgPlayerGoals = relevantPlayerStats.stream()
                .mapToInt(ps -> Optional.ofNullable(ps.getGoalsTotal()).orElse(0))
                .average().orElse(0.0);

        int injuryCount = injuriesByFixture.getOrDefault(contextFixture.getId(), Collections.emptyMap())
                .getOrDefault(teamId, 0L).intValue();

        // Returner det nye objektet med den nye featuren
        return new TeamFeatureSet(avgShotsOnGoal, avgShotsOffGoal, avgCorners, injuryCount, avgPlayerRating, avgPlayerGoals, avgPossession);
    }

    // Oppdater TeamFeatureSet for å inkludere possession
    private static class TeamFeatureSet {
        double avgShotsOnGoal = 0.0, avgShotsOffGoal = 0.0, avgCorners = 0.0;
        int injuryCount = 0;
        double avgPlayerRating = 0.0, avgPlayerGoals = 0.0;
        double avgPossession = 0.0; // <-- NYTT FELT

        TeamFeatureSet() {}

        TeamFeatureSet(double avgSot, double avgSotOff, double avgCorn, int injuries, double avgRating, double avgGoals, double avgPoss) {
            this.avgShotsOnGoal = avgSot;
            this.avgShotsOffGoal = avgSotOff;
            this.avgCorners = avgCorn;
            this.injuryCount = injuries;
            this.avgPlayerRating = avgRating;
            this.avgPlayerGoals = avgGoals;
            this.avgPossession = avgPoss; // <-- NYTT
        }
    }
}