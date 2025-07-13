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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public TrainingDataService(FixtureRepository fixtureRepository, MatchStatisticsRepository matchStatsRepository, InjuryRepository injuryRepository) {
        this.fixtureRepository = fixtureRepository;
        this.matchStatsRepository = matchStatsRepository;
        this.injuryRepository = injuryRepository;
    }

    public List<TrainingDataDto> buildTrainingSet() {
        log.info("Starter bygging av treningssett (Høy-ytelse versjon)...");

        // Hent alle ferdigspilte kamper
        List<Fixture> allCompletedFixtures = fixtureRepository.findByStatusIn(FINISHED_STATUSES);
        log.info("Fant {} ferdigspilte kamper å prosessere.", allCompletedFixtures.size());

        List<TrainingDataDto> trainingSet = new ArrayList<>();

        // Loop gjennom hver kamp for å lage en rad i treningssettet
        for (Fixture fixture : allCompletedFixtures) {
            if (fixture.getGoalsHome() == null || fixture.getGoalsAway() == null) continue;

            TrainingDataDto dto = new TrainingDataDto();
            dto.setFixtureId(fixture.getId());
            dto.setLeagueId(fixture.getLeagueId());
            dto.setSeason(fixture.getSeason());

            // Hent features for hjemmelaget, basert på historikk FØR denne kampen
            TeamFeatureSet homeFeatures = calculateFeaturesForTeam(fixture.getHomeTeamId(), fixture);
            dto.setHomeAvgShotsOnGoal(homeFeatures.avgShotsOnGoal);
            dto.setHomeAvgShotsOffGoal(homeFeatures.avgShotsOffGoal);
            dto.setHomeAvgCorners(homeFeatures.avgCorners);
            dto.setHomeInjuries(homeFeatures.injuryCount);

            // Hent features for bortelaget
            TeamFeatureSet awayFeatures = calculateFeaturesForTeam(fixture.getAwayTeamId(), fixture);
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

        log.info("Fullførte bygging av treningssett med {} rader.", trainingSet.size());
        return trainingSet;
    }

    /**
     * Dette er den logisk korrekte, men trege, metoden.
     * Vi beholder denne inntil videre, og optimaliserer den i et senere steg.
     */
    private TeamFeatureSet calculateFeaturesForTeam(Integer teamId, Fixture contextFixture) {
        // 1. Hent historiske kamper for laget FØR datoen til kontekst-kampen
        List<Fixture> lastFixturesThisSeason = fixtureRepository.findLastNCompletedFixturesByTeamBeforeDate(
                teamId, contextFixture.getDate(), PageRequest.of(0, FORM_MATCH_COUNT_FOR_TRAINING)
        );

        List<Fixture> combinedFixtures = new ArrayList<>(lastFixturesThisSeason);

        // 2. Hvis vi har for få kamper, fyll på med kamper fra forrige sesong
        int neededFromLastSeason = FORM_MATCH_COUNT_FOR_TRAINING - combinedFixtures.size();
        if (neededFromLastSeason > 0 && contextFixture.getSeason() != null) {
            List<Fixture> lastFixturesPrevSeason = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(
                    teamId, contextFixture.getSeason() - 1, PageRequest.of(0, neededFromLastSeason)
            );
            combinedFixtures.addAll(lastFixturesPrevSeason);
        }

        if (combinedFixtures.isEmpty()) {
            return new TeamFeatureSet();
        }

        List<Long> fixtureIds = combinedFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        // GJØR DATABASEKALL INNE I LØKKEN (dette er tregt, men korrekt)
        List<MatchStatistics> stats = matchStatsRepository.findAllByFixtureIdIn(fixtureIds);

        // Beregn gjennomsnittlig statistikk
        double avgShotsOnGoal = stats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOnGoal() != null).mapToDouble(MatchStatistics::getShotsOnGoal).average().orElse(0.0);
        double avgShotsOffGoal = stats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getShotsOffGoal() != null).mapToDouble(MatchStatistics::getShotsOffGoal).average().orElse(0.0);
        double avgCorners = stats.stream().filter(s -> s.getTeamId().equals(teamId) && s.getCornerKicks() != null).mapToDouble(MatchStatistics::getCornerKicks).average().orElse(0.0);

        // Hent antall skader for denne spesifikke kampen
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
}