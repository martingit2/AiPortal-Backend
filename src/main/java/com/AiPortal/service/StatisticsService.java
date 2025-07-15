// src/main/java/com/AiPortal/service/StatisticsService.java
package com.AiPortal.service;

import com.AiPortal.dto.HeadToHeadStatsDto;
import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.MatchStatisticsDto;
import com.AiPortal.dto.PlayerMatchStatisticsDto;
import com.AiPortal.dto.TeamStatisticsDto;
import com.AiPortal.entity.Fixture;
import com.AiPortal.entity.HeadToHeadStats;
import com.AiPortal.entity.MatchStatistics;
import com.AiPortal.entity.Player;
import com.AiPortal.entity.PlayerMatchStatistics;
import com.AiPortal.entity.TeamStatistics;
import com.AiPortal.repository.FixtureRepository;
import com.AiPortal.repository.HeadToHeadStatsRepository;
import com.AiPortal.repository.MatchStatisticsRepository;
import com.AiPortal.repository.PlayerMatchStatisticsRepository;
import com.AiPortal.repository.PlayerRepository;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final TeamStatisticsRepository teamStatisticsRepository;
    private final MatchStatisticsRepository matchStatisticsRepository;
    private final FixtureRepository fixtureRepository;
    private final PlayerMatchStatisticsRepository playerMatchStatsRepository;
    private final PlayerRepository playerRepository;
    private final HeadToHeadStatsRepository h2hStatsRepository;

    @Autowired
    public StatisticsService(
            TeamStatisticsRepository teamStatisticsRepository,
            MatchStatisticsRepository matchStatisticsRepository,
            FixtureRepository fixtureRepository,
            PlayerMatchStatisticsRepository playerMatchStatsRepository,
            PlayerRepository playerRepository,
            HeadToHeadStatsRepository h2hStatsRepository // <-- Ny
    ) {
        this.teamStatisticsRepository = teamStatisticsRepository;
        this.matchStatisticsRepository = matchStatisticsRepository;
        this.fixtureRepository = fixtureRepository;
        this.playerMatchStatsRepository = playerMatchStatsRepository;
        this.playerRepository = playerRepository;
        this.h2hStatsRepository = h2hStatsRepository; // <-- Ny
    }

    public List<PlayerMatchStatisticsDto> getPlayerStatisticsForFixture(Long fixtureId) {
        List<PlayerMatchStatistics> stats = playerMatchStatsRepository.findAllByFixtureId(fixtureId);
        if (stats.isEmpty()) {
            return List.of();
        }

        List<Integer> playerIds = stats.stream().map(PlayerMatchStatistics::getPlayerId).collect(Collectors.toList());
        Map<Integer, Player> playerMap = playerRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(Player::getId, Function.identity()));

        return stats.stream()
                .map(stat -> convertPlayerStatsToDto(stat, playerMap.get(stat.getPlayerId())))
                .collect(Collectors.toList());
    }

    public List<MatchStatistics> getFormStatsForTeam(Integer teamId, Integer season, int limit) {
        List<Fixture> lastFixtures = fixtureRepository.findLastNCompletedFixturesByTeamAndSeason(
                teamId, season, PageRequest.of(0, limit)
        );
        if (lastFixtures.isEmpty()) {
            return List.of();
        }
        List<Long> fixtureIds = lastFixtures.stream().map(Fixture::getId).collect(Collectors.toList());
        return matchStatisticsRepository.findAllByFixtureIdIn(fixtureIds).stream()
                .filter(stat -> stat.getTeamId().equals(teamId))
                .collect(Collectors.toList());
    }

    public List<LeagueStatsGroupDto> getGroupedTeamStatistics() {
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

    /**
     * NY METODE: Henter H2H-statistikk for en gitt kamp.
     * @param fixtureId ID-en til kampen.
     * @return En Optional som inneholder DTO-en hvis data finnes.
     */
    public Optional<HeadToHeadStatsDto> getHeadToHeadStats(Long fixtureId) {
        // Bruker 'findAllByFixtureIdIn' for å dra nytte av EAGER fetching satt opp i entiteten om mulig
        // og for å holde API-et konsistent med andre metoder.
        return h2hStatsRepository.findAllByFixtureIdIn(List.of(fixtureId)).stream()
                .findFirst()
                .map(this::convertToH2HDto);
    }

    private HeadToHeadStatsDto convertToH2HDto(HeadToHeadStats stats) {
        return new HeadToHeadStatsDto(
                stats.getMatchesPlayed(),
                stats.getTeam1Wins(),
                stats.getTeam2Wins(),
                stats.getDraws(),
                stats.getAvgTotalGoals()
        );
    }

    public Optional<TeamStatisticsDto> getTeamInfo(Integer teamId) {
        return teamStatisticsRepository.findTopByTeamId(teamId)
                .map(this::convertToDto);
    }

    private TeamStatisticsDto convertToDto(TeamStatistics stats) {
        return new TeamStatisticsDto(
                stats.getId(), stats.getTeamId(), stats.getTeamName(), stats.getLeagueName(),
                stats.getSeason(), stats.getPlayedTotal(), stats.getWinsTotal(), stats.getDrawsTotal(),
                stats.getLossesTotal(), stats.getGoalsForTotal(), stats.getGoalsAgainstTotal(),
                stats.getSourceBot() != null ? stats.getSourceBot().getName() : "Ukjent Kilde"
        );
    }

    private MatchStatisticsDto convertMatchStatsToDto(MatchStatistics stats) {
        // En mer robust måte å finne lagnavn på, unngår Optional-problemer
        String teamName = teamStatisticsRepository.findTopByTeamId(stats.getTeamId())
                .map(TeamStatistics::getTeamName)
                .orElseGet(() -> fixtureRepository.findById(stats.getFixtureId())
                        .map(f -> f.getHomeTeamId().equals(stats.getTeamId()) ? f.getHomeTeamName() : f.getAwayTeamName())
                        .orElse("Ukjent Lag"));

        return new MatchStatisticsDto(
                teamName, stats.getShotsOnGoal(), stats.getShotsOffGoal(), stats.getTotalShots(),
                stats.getBlockedShots(), stats.getShotsInsideBox(), stats.getShotsOutsideBox(),
                stats.getFouls(), stats.getCornerKicks(), stats.getOffsides(),
                stats.getBallPossession(), stats.getYellowCards(), stats.getRedCards(),
                stats.getGoalkeeperSaves(), stats.getTotalPasses(), stats.getPassesAccurate(),
                stats.getPassesPercentage()
        );
    }

    private PlayerMatchStatisticsDto convertPlayerStatsToDto(PlayerMatchStatistics stat, Player player) {
        PlayerMatchStatisticsDto dto = new PlayerMatchStatisticsDto();
        dto.setPlayerId(stat.getPlayerId());
        dto.setTeamId(stat.getTeamId());
        dto.setPlayerName(player != null ? player.getName() : "Ukjent Spiller");

        dto.setMinutesPlayed(stat.getMinutesPlayed());
        dto.setRating(stat.getRating());
        dto.setCaptain(stat.isCaptain());
        dto.setSubstitute(stat.isSubstitute());
        dto.setShotsTotal(stat.getShotsTotal());
        dto.setShotsOnGoal(stat.getShotsOnGoal());
        dto.setGoalsTotal(stat.getGoalsTotal());
        dto.setAssists(stat.getAssists());
        dto.setPassesTotal(stat.getPassesTotal());
        dto.setPassesKey(stat.getPassesKey());

        return dto;
    }
}