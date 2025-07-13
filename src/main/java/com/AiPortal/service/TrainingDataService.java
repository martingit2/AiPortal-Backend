// src/main/java/com/AiPortal/service/TrainingDataService.java
package com.AiPortal.service;

import com.AiPortal.dto.TrainingDataDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.Injury;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.PlayerMatchStatistics;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.InjuryRepository;
import com.AiPortal.repository.MatchStatisticsRepository;
import com.AiPortal.repository.PlayerMatchStatisticsRepository;
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

    public TrainingDataService(FixtureRepository fixtureRepository, MatchStatisticsRepository matchStatsRepository, InjuryRepository injuryRepository, PlayerMatchStatisticsRepository playerMatchStatsRepository) {
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
        this.playerMatchStatsRepository = playerMatchStatsRepository;
    }

    public List<TrainingDataDto> buildTrainingSet() {
        log.info("---[SPILLERDATA-OPPDATERT] Starter bygging av treningssett ---");

        List<Fixture> allCompletedFixtures = fixtureRepository.findByStatusIn(FINISHED_STATUSES);
        log.info("Fant {} ferdigspilte kamper.", allCompletedFixtures.size());
        if (allCompletedFixtures.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> allFixtureIds = allCompletedFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        List<MatchStatistics> allTeamStats = matchStatsRepository.findAllByFixtureIdIn(allFixtureIds);
        log.info("Fant {} rader med lag-statistikk.", allTeamStats.size());

        List<PlayerMatchStatistics> allPlayerStats = playerMatchStatsRepository.findAllByFixtureIdIn(allFixtureIds);
        log.info("Fant {} rader med spiller-statistikk.", allPlayerStats.size());

        List<Injury> allInjuries = injuryRepository.findAllByFixtureIdIn(allFixtureIds);
        log.info("Fant {} skadeoppføringer.", allInjuries.size());

        Map<Long, List<MatchStatistics>> teamStatsByFixtureId = allTeamStats.stream()
                .collect(Collectors.groupingBy(MatchStatistics::getFixtureId));

        Map<Long, List<PlayerMatchStatistics>> playerStatsByFixtureId = allPlayerStats.stream()
                .collect(Collectors.groupingBy(PlayerMatchStatistics::getFixtureId));

        Map<Long, Map<Integer, Long>> injuriesByFixtureAndTeam = allInjuries.stream()
                .collect(Collectors.groupingBy(Injury::getFixtureId,
                        Collectors.groupingBy(Injury::getTeamId, Collectors.counting())));

        Map<Integer, List<Fixture>> fixturesByTeamId = new HashMap<>();
        for (Fixture f : allCompletedFixtures) {
            fixturesByTeamId.computeIfAbsent(f.getHomeTeamId(), k -> new ArrayList<>()).add(f);
            fixturesByTeamId.computeIfAbsent(f.getAwayTeamId(), k -> new ArrayList<>()).add(f);
        }
        fixturesByTeamId.values().forEach(list -> list.sort(Comparator.comparing(Fixture::getDate).reversed()));

        log.info("Data er pre-prosessert og mappet. Starter feature engineering i minnet.");

        List<TrainingDataDto> trainingSet = new ArrayList<>();
        for (Fixture fixture : allCompletedFixtures) {
            if (fixture.getGoalsHome() == null || fixture.getGoalsAway() == null) continue;

            TrainingDataDto dto = new TrainingDataDto();
            dto.setFixtureId(fixture.getId());
            dto.setLeagueId(fixture.getLeagueId());
            dto.setSeason(fixture.getSeason());

            TeamFeatureSet homeFeatures = calculateFeaturesForTeamInMemory(
                    fixture.getHomeTeamId(), fixture, fixturesByTeamId, teamStatsByFixtureId, playerStatsByFixtureId, injuriesByFixtureAndTeam
            );
            dto.setHomeAvgShotsOnGoal(homeFeatures.avgShotsOnGoal);
            dto.setHomeAvgShotsOffGoal(homeFeatures.avgShotsOffGoal);
            dto.setHomeAvgCorners(homeFeatures.avgCorners);
            dto.setHomeInjuries(homeFeatures.injuryCount);
            dto.setHomePlayersAvgRating(homeFeatures.avgPlayerRating);
            dto.setHomePlayersAvgGoals(homeFeatures.avgPlayerGoals);

            TeamFeatureSet awayFeatures = calculateFeaturesForTeamInMemory(
                    fixture.getAwayTeamId(), fixture, fixturesByTeamId, teamStatsByFixtureId, playerStatsByFixtureId, injuriesByFixtureAndTeam
            );
            dto.setAwayAvgShotsOnGoal(awayFeatures.avgShotsOnGoal);
            dto.setAwayAvgShotsOffGoal(awayFeatures.avgShotsOffGoal);
            dto.setAwayAvgCorners(awayFeatures.avgCorners);
            dto.setAwayInjuries(awayFeatures.injuryCount);
            dto.setAwayPlayersAvgRating(awayFeatures.avgPlayerRating);
            dto.setAwayPlayersAvgGoals(awayFeatures.avgPlayerGoals);

            if (fixture.getGoalsHome() > fixture.getGoalsAway()) dto.setResult("HOME_WIN");
            else if (fixture.getGoalsAway() > fixture.getGoalsHome()) dto.setResult("AWAY_WIN");
            else dto.setResult("DRAW");

            trainingSet.add(dto);
        }

        log.info("Fullførte bygging av treningssett med {} rader. Antall DB-kall: 4", trainingSet.size());
        return trainingSet;
    }

    private TeamFeatureSet calculateFeaturesForTeamInMemory(
            Integer teamId,
            Fixture contextFixture,
            Map<Integer, List<Fixture>> fixturesByTeam,
            Map<Long, List<MatchStatistics>> teamStatsByFixture,
            Map<Long, List<PlayerMatchStatistics>> playerStatsByFixture,
            Map<Long, Map<Integer, Long>> injuriesByFixture
    ) {
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

        double avgShotsOnGoal = relevantTeamStats.stream().mapToInt(s -> Optional.ofNullable(s.getShotsOnGoal()).orElse(0)).average().orElse(0.0);
        double avgShotsOffGoal = relevantTeamStats.stream().mapToInt(s -> Optional.ofNullable(s.getShotsOffGoal()).orElse(0)).average().orElse(0.0);
        double avgCorners = relevantTeamStats.stream().mapToInt(s -> Optional.ofNullable(s.getCornerKicks()).orElse(0)).average().orElse(0.0);

        List<PlayerMatchStatistics> relevantPlayerStats = pastFixtures.stream()
                .flatMap(f -> playerStatsByFixture.getOrDefault(f.getId(), Collections.emptyList()).stream())
                .filter(ps -> ps.getTeamId().equals(teamId))
                .collect(Collectors.toList());

        double avgPlayerRating = relevantPlayerStats.stream()
                .map(ps -> {
                    try {
                        return ps.getRating() != null ? Double.parseDouble(ps.getRating()) : 0.0;
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double avgPlayerGoals = relevantPlayerStats.stream()
                .mapToInt(ps -> Optional.ofNullable(ps.getGoalsTotal()).orElse(0))
                .average()
                .orElse(0.0);

        int injuryCount = injuriesByFixture.getOrDefault(contextFixture.getId(), Collections.emptyMap())
                .getOrDefault(teamId, 0L).intValue();

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
}