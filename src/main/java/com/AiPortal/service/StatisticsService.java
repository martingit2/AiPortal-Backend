// src/main/java/com/AiPortal/service/StatisticsService.java
package com.AiPortal.service;

import com.AiPortal.dto.LeagueStatsGroupDto;
import com.AiPortal.dto.TeamStatisticsDto;
import com.AiPortal.entity.TeamStatistics;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service-lag for å håndtere logikk relatert til statistikk.
 */
@Service
@Transactional(readOnly = true) // Alle metoder her er kun for lesing
public class StatisticsService {

    private final TeamStatisticsRepository statsRepository;

    @Autowired
    public StatisticsService(TeamStatisticsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    /**
     * Henter all lagret lagstatistikk og returnerer den gruppert etter liga og sesong.
     * Gruppene er sortert med den nyeste sesongen og deretter alfabetisk på liganavn.
     * @return En liste av LeagueStatsGroupDto-objekter.
     */
    public List<LeagueStatsGroupDto> getGroupedTeamStatistics() {
        // Hent all statistikk fra databasen
        List<TeamStatistics> allStats = statsRepository.findAll();

        // Grupperer statistikken ved å bruke Java Streams.
        return allStats.stream()
                .collect(Collectors.groupingBy(
                        // 1. Lag en grupperingsnøkkel som "Premier League - 2023"
                        stats -> stats.getLeagueName() + " - " + stats.getSeason(),
                        // 2. For hver gruppe, konverter entitetene til DTO-er
                        Collectors.mapping(this::convertToDto, Collectors.toList())
                ))
                .entrySet().stream()
                // 3. Konverter det resulterende Map-et til en liste av LeagueStatsGroupDto-objekter
                .map(entry -> new LeagueStatsGroupDto(entry.getKey(), entry.getValue()))
                // 4. Sorter den endelige listen av grupper for en konsistent visning i frontend
                .sorted(Comparator.comparing(LeagueStatsGroupDto::getGroupTitle).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Hjelpemetode for å konvertere en TeamStatistics-entitet til en TeamStatisticsDto.
     * Dette skiller database-laget fra API-laget.
     * @param stats Entiteten som skal konverteres.
     * @return Den konverterte DTOen.
     */
    private TeamStatisticsDto convertToDto(TeamStatistics stats) {
        return new TeamStatisticsDto(
                stats.getId(),
                stats.getTeamName(),
                stats.getLeagueName(),
                stats.getSeason(),
                stats.getPlayedTotal(),
                stats.getWinsTotal(),
                stats.getDrawsTotal(),
                stats.getLossesTotal(),
                stats.getGoalsForTotal(),
                stats.getGoalsAgainstTotal(),
                // Håndterer tilfellet der en bot er slettet, men statistikken gjenstår
                stats.getSourceBot() != null ? stats.getSourceBot().getName() : "Ukjent Kilde"
        );
    }
}