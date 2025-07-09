// src/main/java/com/AiPortal/service/StatisticsService.java
package com.AiPortal.service;

import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.MatchStatisticsDto;
import com.AiPortal.dto.TeamStatisticsDto;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.TeamStatistics;
import com.AiPortal.repository.MatchStatisticsRepository;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final TeamStatisticsRepository teamStatisticsRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;

    @Autowired
    public StatisticsService(TeamStatisticsRepository teamStatisticsRepository, MatchStatisticsRepository matchStatisticsRepository) {
        this.teamStatisticsRepository = teamStatisticsRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
    }

    public List<LeagueStatsGroupDto> getGroupedTeamStatistics() {
        List<TeamStatistics> allStats = teamStatisticsRepository.findAll();

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

    // ---- START PÅ OPPDATERT HJELPEMETODE ----
    /**
     * Hjelpemetode for å konvertere en TeamStatistics-entitet til en TeamStatisticsDto.
     * Den inkluderer nå den faktiske teamId for navigering i frontend.
     */
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
                stats.getSourceBot() != null ? stats.getSourceBot().getName() : "Ukjent Kilde"
        );
    }


    private MatchStatisticsDto convertMatchStatsToDto(MatchStatistics stats) {
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