package com.AiPortal.service;

import com.AiPortal.dto.TeamStatisticsDto;
import com.AiPortal.repository.TeamStatisticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.AiPortal.entity.TeamStatistics;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatisticsService {

    private final TeamStatisticsRepository statsRepository;

    @Autowired
    public StatisticsService(TeamStatisticsRepository statsRepository) {
        this.statsRepository = statsRepository;
    }

    /**
     * Henter all lagret lagstatistikk og konverterer den til DTOs.
     * @return En liste av TeamStatisticsDto.
     */
    public List<TeamStatisticsDto> getAllTeamStatistics() {
        List<TeamStatistics> allStats = statsRepository.findAll(Sort.by("teamName").ascending());

        // Konverterer listen av Entiteter til en liste av DTOs
        return allStats.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Hjelpemetode for å konvertere en TeamStatistics-entitet til en TeamStatisticsDto.
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
                stats.getSourceBot() != null ? stats.getSourceBot().getName() : "Ukjent"
        );
    }
}