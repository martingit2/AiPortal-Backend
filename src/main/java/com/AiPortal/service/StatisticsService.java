// src/main/java/com/AiPortal/service/StatisticsService.java
package com.AiPortal.service;

import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.MatchStatisticsDto;
import com.AiPortal.dto.TeamStatisticsDto;
import com.AiPortal.entity.Fixture; // Importer Fixture
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.TeamStatistics;
import com.AiPortal.repository.FixtureRepository; // Importer FixtureRepository
import com.AiPortal.repository.MatchStatisticsRepository;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest; // Importer PageRequest
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final TeamStatisticsRepository teamStatisticsRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final FixtureRepository fixtureRepository;

    @Autowired
    public StatisticsService(TeamStatisticsRepository teamStatisticsRepository, MatchStatisticsRepository matchStatisticsRepository, FixtureRepository fixtureRepository) {
        this.teamStatisticsRepository = teamStatisticsRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.fixtureRepository = fixtureRepository;
    }

    /**
     * Henter statistikk for de siste N kampene for et lag for å bygge en formgraf.
     * @param teamId ID-en til laget.
     * @param season Sesongen.
     * @param limit Antall kamper å hente.
     * @return En liste med MatchStatistics-entiteter for det gitte laget.
     */
    public List<MatchStatistics> getFormStatsForTeam(Integer teamId, Integer season, int limit) {
        // 1. Finn de 'limit' siste ferdigspilte kampene for laget
        List<Fixture> lastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(
                teamId, season, PageRequest.of(0, limit)
        );

        if (lastFixtures.isEmpty()) {
            return List.of(); // Returner tom liste hvis ingen kamper finnes
        }

        // 2. Hent ut alle kamp-IDene
        List<Long> fixtureIds = lastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());

        // 3. Hent all statistikk for disse kampene og filtrer for kun det aktuelle laget
        return matchStatisticsRepository.findAllByFixtureIdIn(fixtureIds).stream()
                .filter(stat -> stat.getTeamId().equals(teamId))
                .collect(Collectors.toList());
    }


    public List<LeagueStatsGroupDto> getGroupedTeamStatistics() {
        // --- OPTIMALISERING: Bruker den nye metoden fra repository-et ---
        // Dette henter TeamStatistics OG tilhørende BotConfiguration i én enkelt database-spørring,
        // som løser "N+1 select"-problemet og er dramatisk mye raskere.
        List<TeamStatistics> allStats = teamStatisticsRepository.findAllWithBot();

        return allStats.stream()
                .collect(Collectors.groupingBy(
                        stats -> stats.getLeagueName() + " - " + stats.getSeason(),
                        Collectors.mapping(this::convertToDto, Collectors.toList())
                ))
                .entrySet().stream()
                .map(entry -> new LeagueStatsGroupDto(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(LeagueStatsGroupDto::getGroupTitle).reversed())
                .collect(Collectors.toList());
    }

    public List<MatchStatisticsDto> getStatisticsForFixture(Long fixtureId) {
        List<MatchStatistics> stats = matchStatisticsRepository.findAllByFixtureId(fixtureId);
        return stats.stream()
                .map(this::convertMatchStatsToDto)
                .collect(Collectors.toList());
    }

    public Optional<TeamStatisticsDto> getTeamInfo(Integer teamId) {
        return teamStatisticsRepository.findTopByTeamId(teamId)
                .map(this::convertToDto);
    }

    private TeamStatisticsDto convertToDto(TeamStatistics stats) {
        return new TeamStatisticsDto(
                stats.getId(),
                stats.getTeamId(),
                stats.getTeamName(),
                stats.getLeagueName(),
                stats.getSeason(),
                stats.getPlayedTotal(),
                stats.getWinsTotal(),
                stats.getDrawsTotal(),
                stats.getLossesTotal(),
                stats.getGoalsForTotal(),
                stats.getGoalsAgainstTotal(),
                // Dette er nå trygt og raskt, siden sourceBot allerede er lastet inn
                stats.getSourceBot() != null ? stats.getSourceBot().getName() : "Ukjent Kilde"
        );
    }

    private MatchStatisticsDto convertMatchStatsToDto(MatchStatistics stats) {
        // Denne metoden kan fortsatt forårsake et ekstra kall per lag.
        // For optimalisering her, kunne man hentet alle team-navn i en egen bulk-operasjon først.
        // Men forbedringen er mindre kritisk enn i getGroupedTeamStatistics.
        String teamName = teamStatisticsRepository.findTopByTeamId(stats.getTeamId())
                .map(TeamStatistics::getTeamName)
                .orElse("Ukjent Lag");

        return new MatchStatisticsDto(
                teamName,
                stats.getShotsOnGoal(),
                stats.getShotsOffGoal(),
                stats.getTotalShots(),
                stats.getBlockedShots(),
                stats.getShotsInsideBox(),
                stats.getShotsOutsideBox(),
                stats.getFouls(),
                stats.getCornerKicks(),
                stats.getOffsides(),
                stats.getBallPossession(),
                stats.getYellowCards(),
                stats.getRedCards(),
                stats.getGoalkeeperSaves(),
                stats.getTotalPasses(),
                stats.getPassesAccurate(),
                stats.getPassesPercentage()
        );
    }
}