// src/main/java/com/AiPortal/dto/LeagueStatsGroupDto.java
package com.AiPortal.dto;

import java.util.List;

public class LeagueStatsGroupDto {
    private String groupTitle; // F.eks. "Premier League - 2023"
    private List<TeamStatisticsDto> statistics;

    public LeagueStatsGroupDto(String groupTitle, List<TeamStatisticsDto> statistics) {
        this.groupTitle = groupTitle;
        this.statistics = statistics;
    }

    // Getters
    public String getGroupTitle() { return groupTitle; }
    public List<TeamStatisticsDto> getStatistics() { return statistics; }
}