// src/main/java/com/AiPortal/service/TrainingDataService.java
package com.AiPortal.service;

import com.AiPortal.dto.TrainingDataDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.Injury;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.InjuryRepository;
import com.AiPortal.repository.MatchStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
public class TrainingDataService {

    private static final Logger log = LoggerFactory.getLogger(TrainingDataService.class);
    private static final int FORM_MATCH_COUNT_FOR_TRAINING = 10;
    private static final List<String> FINISHED_STATUSES = List.of("FT", "AET", "PEN");

    private final FixtureRepository fixtureRepository;
    private final MatchStatisticsRepository matchStatsRepository;
    private final InjuryRepository injuryRepository;

    public TrainingDataService(FixtureRepository fixtureRepository, MatchStatisticsRepository matchStatsRepository, InjuryRepository injuryRepository) {
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
    }

    /**
     * Bygger et komplett treningssett ved å først hente all nødvendig data i bulk,
     * for deretter å prosessere den i minnet. Dette er dramatisk raskere enn
     * å gjøre databasekall inne i en løkke.
     */
    public List<TrainingDataDto> buildTrainingSet() {
        log.info("---[OPTIMALISERT] Starter bygging av treningssett ---");

        // --- STEG 1: HENT ALL NØDVENDIG DATA I BULK ---

        // 1a. Hent alle ferdigspilte kamper. Dette er utgangspunktet.
        List<Fixture> allCompletedFixtures = fixtureRepository.findByStatusIn(FINISHED_STATUSES);
        log.info("Fant {} ferdigspilte kamper.", allCompletedFixtures.size());
        if (allCompletedFixtures.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> allFixtureIds = allCompletedFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        // 1b. Hent all relevant kampstatistikk for alle kampene i ett kall.
        List<MatchStatistics> allStats = matchStatsRepository.findAllByFixtureIdIn(allFixtureIds);
        log.info("Fant {} rader med kampstatistikk for disse kampene.", allStats.size());

        // 1c. Hent all relevant skadeinformasjon for alle kampene i ett kall.
        List<Injury> allInjuries = injuryRepository.findAllByFixtureIdIn(allFixtureIds);
        log.info("Fant {} skadeoppføringer for disse kampene.", allInjuries.size());

        // --- STEG 2: PROSESSER BULK-DATA TIL EFFEKTIVE MAPS FOR RASKT OPPSLAG ---

        // Map fra fixtureId -> List<MatchStatistics>
        Map<Long, List<MatchStatistics>> statsByFixtureId = allStats.stream()
                .collect(Collectors.groupingBy(MatchStatistics::getFixtureId));

        // Map fra fixtureId -> teamId -> antall skader
        Map<Long, Map<Integer, Long>> injuriesByFixtureAndTeam = allInjuries.stream()
                .collect(Collectors.groupingBy(Injury::getFixtureId,
                        Collectors.groupingBy(Injury::getTeamId, Collectors.counting())));

        // Map fra teamId -> List<Fixture> (sortert etter dato, nyeste først)
        Map<Integer, List<Fixture>> fixturesByTeamId = new HashMap<>();
        for (Fixture f : allCompletedFixtures) {
            fixturesByTeamId.computeIfAbsent(f.getHomeTeamId(), k -> new ArrayList<>()).add(f);
            fixturesByTeamId.computeIfAbsent(f.getAwayTeamId(), k -> new ArrayList<>()).add(f);
        }
        // Sorter listene én gang for alle
        fixturesByTeamId.values().forEach(list -> list.sort(Comparator.comparing(Fixture::getDate).reversed()));

        log.info("Data er pre-prosessert og mappet. Starter feature engineering i minnet.");

        // --- STEG 3: ITERER OG BYGG TRENINGSSETTET VED HJELP AV DE RASKE MAPSENE ---

        List<TrainingDataDto> trainingSet = new ArrayList<>();
        for (Fixture fixture : allCompletedFixtures) {
            if (fixture.getGoalsHome() == null || fixture.getGoalsAway() == null) continue;

            TrainingDataDto dto = new TrainingDataDto();
            dto.setFixtureId(fixture.getId());
            dto.setLeagueId(fixture.getLeagueId());
            dto.setSeason(fixture.getSeason());

            // Hent features for hjemmelaget (bruker nå in-memory-data)
            TeamFeatureSet homeFeatures = calculateFeaturesForTeamInMemory(
                    fixture.getHomeTeamId(), fixture, fixturesByTeamId, statsByFixtureId, injuriesByFixtureAndTeam
            );
            dto.setHomeAvgShotsOnGoal(homeFeatures.avgShotsOnGoal);
            dto.setHomeAvgShotsOffGoal(homeFeatures.avgShotsOffGoal);
            dto.setHomeAvgCorners(homeFeatures.avgCorners);
            dto.setHomeInjuries(homeFeatures.injuryCount);

            // Hent features for bortelaget
            TeamFeatureSet awayFeatures = calculateFeaturesForTeamInMemory(
                    fixture.getAwayTeamId(), fixture, fixturesByTeamId, statsByFixtureId, injuriesByFixtureAndTeam
            );
            dto.setAwayAvgShotsOnGoal(awayFeatures.avgShotsOnGoal);
            dto.setAwayAvgShotsOffGoal(awayFeatures.avgShotsOffGoal);
            dto.setAwayAvgCorners(awayFeatures.avgCorners);
            dto.setAwayInjuries(awayFeatures.injuryCount);

            // Sett resultat (label)
            if (fixture.getGoalsHome() > fixture.getGoalsAway()) dto.setResult("HOME_WIN");
            else if (fixture.getGoalsAway() > fixture.getGoalsHome()) dto.setResult("AWAY_WIN");
            else dto.setResult("DRAW");

            trainingSet.add(dto);
        }

        log.info("Fullførte bygging av treningssett med {} rader. Antall DB-kall: 3", trainingSet.size());
        return trainingSet;
    }

    /**
     * Hjelpemetode som beregner features for et lag utelukkende basert på
     * pre-lastet data i minnet. Gjør ingen databasekall.
     */
    private TeamFeatureSet calculateFeaturesForTeamInMemory(
            Integer teamId,
            Fixture contextFixture,
            Map<Integer, List<Fixture>> fixturesByTeam,
            Map<Long, List<MatchStatistics>> statsByFixture,
            Map<Long, Map<Integer, Long>> injuriesByFixture
    ) {
        // Finn lagets kamper i det pre-lastede map-et
        List<Fixture> teamFixtures = fixturesByTeam.getOrDefault(teamId, Collections.emptyList());

        // Finn alle kamper spilt FØR den aktuelle kampen (contextFixture)
        List<Fixture> pastFixtures = teamFixtures.stream()
                .filter(f -> f.getDate().isBefore(contextFixture.getDate()))
                .limit(FORM_MATCH_COUNT_FOR_TRAINING) // Ta de 10 siste
                .collect(Collectors.toList());

        if (pastFixtures.isEmpty()) {
            return new TeamFeatureSet(); // Returner tomt objekt hvis ingen historikk
        }

        // Hent statistikk for disse kampene fra det pre-lastede map-et
        List<MatchStatistics> relevantStats = pastFixtures.stream()
                .flatMap(f -> statsByFixture.getOrDefault(f.getId(), Collections.emptyList()).stream())
                .filter(s -> s.getTeamId().equals(teamId))
                .collect(Collectors.toList());

        // Beregn gjennomsnitt
        double avgShotsOnGoal = relevantStats.stream().mapToInt(s -> Optional.ofNullable(s.getShotsOnGoal()).orElse(0)).average().orElse(0.0);
        double avgShotsOffGoal = relevantStats.stream().mapToInt(s -> Optional.ofNullable(s.getShotsOffGoal()).orElse(0)).average().orElse(0.0);
        double avgCorners = relevantStats.stream().mapToInt(s -> Optional.ofNullable(s.getCornerKicks()).orElse(0)).average().orElse(0.0);

        // Hent antall skader for denne spesifikke kampen fra det pre-lastede map-et
        int injuryCount = injuriesByFixture.getOrDefault(contextFixture.getId(), Collections.emptyMap())
                .getOrDefault(teamId, 0L).intValue();

        return new TeamFeatureSet(avgShotsOnGoal, avgShotsOffGoal, avgCorners, injuryCount);
    }

    /**
     * Enkel indre klasse for å holde på beregnede features for et lag.
     */
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
}